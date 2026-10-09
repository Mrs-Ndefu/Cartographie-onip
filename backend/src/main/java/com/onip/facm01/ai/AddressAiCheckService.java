package com.onip.facm01.ai;

import com.onip.facm01.household.AddressEmbeddable;
import com.onip.facm01.household.Household;
import com.onip.facm01.household.HouseholdRepository;
import com.onip.facm01.household.HouseholdStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

// Vérifie que le quartier/l'avenue/l'immeuble saisis pour un ménage ressemblent à une vraie
// adresse plutôt qu'à du texte au hasard — via l'API Claude (Anthropic) si une clé est
// configurée (cf. AnthropicClient), sinon via une heuristique locale sans dépendance externe ni
// coût (cf. AddressPlausibilityHeuristic) : c'est le cas par défaut, et ça fonctionne
// immédiatement sans aucune configuration. Si l'adresse semble incohérente, le ménage passe en
// statut A_VERIFIER : il disparaît alors du tableau de bord par défaut et déclenche la même
// notification (cloche) que les ménages incomplets (cf. DashboardController, statut "incomplet"
// = tout statut différent de COMPLET), à charge pour un ADMIN/SUPERVISEUR de valider ou rejeter
// depuis la page détail (AgentRole.canReviewHousehold).
@Service
public class AddressAiCheckService {

    private static final Logger log = LoggerFactory.getLogger(AddressAiCheckService.class);

    private final HouseholdRepository householdRepository;
    private final AnthropicClient anthropicClient;
    private final boolean enabled;

    public AddressAiCheckService(
            HouseholdRepository householdRepository,
            AnthropicClient anthropicClient,
            @Value("${app.ai.address-check.enabled:true}") boolean enabled) {
        this.householdRepository = householdRepository;
        this.anthropicClient = anthropicClient;
        this.enabled = enabled;
    }

    // @Async : ne doit jamais ralentir ni faire échouer la synchro/la modification d'origine.
    // AFTER_COMMIT : le ménage doit déjà être visible en base pour le thread asynchrone, qui n'a
    // pas la transaction d'origine (cf. HouseholdService, qui publie l'évènement juste après
    // householdRepository.save).
    // REQUIRES_NEW : la transaction d'origine est déjà validée (AFTER_COMMIT), donc ce traitement
    // a besoin de sa propre transaction (Spring l'exige explicitement pour un listener
    // AFTER_COMMIT annoté @Transactional).
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAddressCheckRequested(AddressCheckRequestedEvent event) {
        if (!enabled) {
            return;
        }
        Household household = householdRepository.findById(event.householdId()).orElse(null);
        if (household == null || household.isArchived() || household.getStatus() != HouseholdStatus.COMPLET) {
            // Déjà traité autrement (archivé, rejeté, repassé en brouillon...) entre la
            // publication de l'évènement et son traitement : rien à vérifier.
            return;
        }

        String signature = signature(household.getAddress());
        if (signature.equals(household.getAddressCheckedSignature())) {
            // Texte d'adresse identique à la dernière vérification : pas la peine de rappeler
            // l'IA, et surtout pas de re-signaler un ménage déjà validé malgré une alerte passée.
            return;
        }
        if (signature.isEmpty()) {
            household.setAddressCheckedSignature(signature);
            householdRepository.save(household);
            return;
        }

        boolean useAi = anthropicClient.hasApiKey();
        try {
            AddressEmbeddable address = household.getAddress();
            boolean implausible = useAi
                    ? anthropicClient.ask(prompt(address)).startsWith("NON")
                    : AddressPlausibilityHeuristic.isImplausible(
                            address.getQuartier(), address.getRue(), address.getImmeuble());
            household.setAddressCheckedSignature(signature);
            if (implausible) {
                household.setStatus(HouseholdStatus.A_VERIFIER);
                log.warn("Adresse jugée peu plausible ({}) pour le ménage {} : passé en 'à vérifier'",
                        useAi ? "IA" : "heuristique locale", household.getId());
            }
            householdRepository.save(household);
        } catch (Exception e) {
            // Panne réseau, clé invalide, quota dépassé... (seulement possible si useAi) : on ne
            // touche pas au ménage (la signature n'est pas enregistrée, un prochain essai
            // retentera) et on ne bloque rien côté utilisateur puisqu'on est déjà dans un thread à
            // part, après coup. L'heuristique locale, elle, ne peut pas échouer de cette façon.
            log.warn("Vérification de l'adresse impossible pour le ménage {} : {}",
                    event.householdId(), e.getMessage());
        }
    }

    private static String prompt(AddressEmbeddable address) {
        return """
                Tu vérifies la plausibilité d'une adresse saisie lors d'un recensement de ménages
                en République Démocratique du Congo (RDC).

                Quartier : "%s"
                Avenue/Rue : "%s"
                Numéro : "%s"
                Immeuble : "%s"

                Réponds uniquement par le mot OUI si ces informations ressemblent, même de façon
                simple, incomplète ou mal orthographiée, à une vraie adresse. Réponds uniquement
                par le mot NON si elles sont manifestement dénuées de sens : une suite de lettres
                au hasard, du texte tapé au clavier sans réfléchir (ex. "qsdqsd", "aaaaa",
                "test123"), ou toute autre absence évidente d'adresse réelle. Un seul mot en
                réponse : OUI ou NON.
                """.formatted(
                orBlank(address.getQuartier()), orBlank(address.getRue()),
                orBlank(address.getNumero()), orBlank(address.getImmeuble()));
    }

    private static String orBlank(String value) {
        return value == null || value.isBlank() ? "(vide)" : value;
    }

    // Empreinte du texte d'adresse pertinent pour ce contrôle (quartier/rue/numéro/immeuble) :
    // sert de clé de cache pour ne pas rappeler l'IA ni re-signaler le ménage si rien n'a changé.
    private static String signature(AddressEmbeddable address) {
        String joined = String.join("|",
                normalize(address.getQuartier()), normalize(address.getRue()),
                normalize(address.getNumero()), normalize(address.getImmeuble()));
        if (joined.replace("|", "").isEmpty()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est garanti disponible sur toute JVM standard : ne devrait jamais arriver.
            throw new IllegalStateException(e);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
