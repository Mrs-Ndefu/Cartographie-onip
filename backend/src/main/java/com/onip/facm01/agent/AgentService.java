package com.onip.facm01.agent;

import com.onip.facm01.household.HouseholdRepository;
import com.onip.facm01.site.SiteRepository;
import com.onip.facm01.zone.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AgentService {

    private static final long MAX_PHOTO_SIZE = 2 * 1024 * 1024; // 2 Mo

    // Format simple mais suffisant pour rejeter les noms d'utilisateur qui ne ressemblent pas
    // à une adresse mail (le nom d'utilisateur EST l'adresse mail de l'agent, pas un pseudo).
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AgentRepository agentRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final HouseholdRepository householdRepository;
    private final SiteRepository siteRepository;
    private final PasswordEncoder passwordEncoder;

    public AgentService(
            AgentRepository agentRepository,
            StaffMemberRepository staffMemberRepository,
            HouseholdRepository householdRepository,
            SiteRepository siteRepository,
            PasswordEncoder passwordEncoder) {
        this.agentRepository = agentRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.householdRepository = householdRepository;
        this.siteRepository = siteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Agent createAgent(String username, String rawPassword, String fullName, AgentRole role) {
        if (!EMAIL_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Le nom d'utilisateur doit être une adresse mail valide");
        }
        if (agentRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Un agent avec ce nom d'utilisateur existe déjà");
        }
        Agent agent = new Agent(UUID.randomUUID(), username, passwordEncoder.encode(rawPassword), fullName, role);
        agent.setGeneratedPassword(rawPassword);
        return agentRepository.save(agent);
    }

    // Création en masse de tous les comptes du personnel importé qui n'en ont pas encore (cf.
    // AgentAdminController, bouton "Créer tous les comptes" sur la liste importée) : un mot de
    // passe différent est généré automatiquement pour chacun (jamais le même pour tout le monde,
    // même en masse — cf. PasswordGenerator), consultable ensuite depuis "Gérer" (cf.
    // Agent.generatedPassword). Le rôle suggéré par le fichier Excel est utilisé s'il est défini,
    // sinon AGENT par défaut. Une ligne déjà pourvue d'un compte (même email) est simplement
    // ignorée, pas remontée en erreur — importer plusieurs fois ne doit jamais faire échouer le
    // reste du lot.
    public record BulkCreationResult(int created, int skipped) {
    }

    public BulkCreationResult createAllFromStaffList() {
        int created = 0;
        int skipped = 0;
        for (StaffMember staff : staffMemberRepository.findAll()) {
            if (agentRepository.existsByUsername(staff.getEmail())) {
                skipped++;
                continue;
            }
            AgentRole role = staff.getRole() != null ? staff.getRole() : AgentRole.AGENT;
            try {
                createAgent(staff.getEmail(), PasswordGenerator.generate(), staff.getFullName(), role);
                created++;
            } catch (IllegalArgumentException e) {
                // Email mal formé dans le fichier Excel, par exemple : on ignore cette ligne
                // plutôt que de faire échouer tout le lot.
                skipped++;
            }
        }
        return new BulkCreationResult(created, skipped);
    }

    // Variante utilisée par la création d'un compte depuis le tableau de bord/l'API (pas par le
    // bootstrap au démarrage, cf. DataSeeder, qui doit toujours réussir) : le nom et l'email
    // saisis doivent correspondre à une ligne du fichier Excel importé par l'ADMIN. Si aucun
    // fichier n'a encore été importé (liste vide), la vérification est ignorée — la fonctionnalité
    // n'est tout simplement pas encore activée.
    public Agent createAgentFromStaffList(String username, String rawPassword, String fullName, AgentRole role) {
        if (staffMemberRepository.count() > 0) {
            boolean matches = staffMemberRepository.findByEmailIgnoreCase(username)
                    .filter(s -> s.getFullName().trim().equalsIgnoreCase(fullName.trim()))
                    .isPresent();
            if (!matches) {
                throw new IllegalArgumentException(
                        "Ce nom et cet email ne correspondent à aucun personnel importé. "
                                + "Vérifiez l'orthographe ou mettez à jour le fichier Excel.");
            }
        }
        return createAgent(username, rawPassword, fullName, role);
    }

    public List<Agent> listAgents() {
        return agentRepository.findAll();
    }

    // search vide = pas de filtre (cf. AgentRepository.search) ; la zone de recherche de
    // agents.html passe toujours une chaîne non nulle (possiblement vide).
    public Page<Agent> listAgents(String search, Pageable pageable) {
        return agentRepository.search(search, pageable);
    }

    // Comptes affectés directement à un superviseur ou un chef d'équipe donné (un seul niveau) —
    // la page "Gérer les agents" d'un CHEF_EQUIPE (ses AGENT).
    public Page<Agent> listAgentsOf(UUID superviseurId, String search, Pageable pageable) {
        return agentRepository.searchBySuperviseur(superviseurId, search, pageable);
    }

    // Équipe complète d'un superviseur (ses chefs d'équipe ET les agents de chacun) — la page
    // "Gérer les agents" d'un SUPERVISEUR : il doit voir les agents pour pouvoir les affecter à
    // un de ses chefs d'équipe (cf. AgentAdminController#assignSupervisor).
    public Page<Agent> listTeamOf(UUID superviseurId, String search, Pageable pageable) {
        return agentRepository.searchByTeamOf(superviseurId, search, pageable);
    }

    public List<Agent> listSupervisors() {
        return agentRepository.findByRoleAndActiveTrueOrderByFullNameAsc(AgentRole.SUPERVISEUR);
    }

    // Chefs d'équipe actifs : alimente le menu déroulant "Encadrant" des comptes AGENT (cf.
    // agents.html) — même rôle que listSupervisors() pour les comptes CHEF_EQUIPE. Vue ADMIN-tier
    // (tous les chefs d'équipe) ; cf. listChefsEquipeOf pour la vue restreinte d'un superviseur.
    public List<Agent> listChefsEquipe() {
        return agentRepository.findByRoleAndActiveTrueOrderByFullNameAsc(AgentRole.CHEF_EQUIPE);
    }

    // Chefs d'équipe d'un superviseur donné uniquement : un superviseur n'affecte un agent qu'à
    // SES propres chefs d'équipe, pas à ceux d'un autre superviseur.
    public List<Agent> listChefsEquipeOf(UUID superviseurId) {
        return agentRepository.findBySuperviseur_IdAndRoleAndActiveTrueOrderByFullNameAsc(superviseurId, AgentRole.CHEF_EQUIPE);
    }

    // Affecte un compte à son encadrant direct : un AGENT à un CHEF_EQUIPE, un CHEF_EQUIPE à un
    // SUPERVISEUR (cf. Agent.superviseur, réutilisé pour les deux niveaux). superviseur == null
    // retire l'affectation.
    public Agent assignSupervisor(UUID id, Agent superviseur) {
        Agent agent = getAgent(id);
        AgentRole expectedRole = switch (agent.getRole()) {
            case AGENT -> AgentRole.CHEF_EQUIPE;
            case CHEF_EQUIPE -> AgentRole.SUPERVISEUR;
            default -> throw new IllegalArgumentException(
                    "Seul un compte AGENT ou CHEF_EQUIPE peut être affecté à un encadrant");
        };
        if (superviseur != null && superviseur.getRole() != expectedRole) {
            throw new IllegalArgumentException("Le compte choisi n'a pas le bon rôle pour encadrer ce compte");
        }
        agent.setSuperviseur(superviseur);
        return agentRepository.save(agent);
    }

    public void ensureAgentExists(String username, String rawPassword, String fullName, AgentRole role) {
        if (!agentRepository.existsByUsername(username)) {
            createAgent(username, rawPassword, fullName, role);
        }
    }

    public Agent getAgent(UUID id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent introuvable"));
    }

    public Agent setActive(UUID id, boolean active) {
        Agent agent = getAgent(id);
        agent.setActive(active);
        return agentRepository.save(agent);
    }

    public Agent changeRole(UUID id, AgentRole role) {
        Agent agent = getAgent(id);
        AgentRole previous = agent.getRole();
        agent.setRole(role);
        if (!role.canHaveZone()) {
            agent.setZone(null);
        }
        // Le lien vers l'encadrant (cf. assignSupervisor) n'a de sens que pour le rôle d'origine
        // (AGENT→CHEF_EQUIPE ou CHEF_EQUIPE→SUPERVISEUR) : un changement de rôle réel l'efface,
        // l'ancien encadrant n'étant plus forcément du bon rôle pour le nouveau niveau — à
        // réaffecter explicitement si besoin. Rien ne change si le rôle soumis est déjà le sien
        // (ex. formulaire resoumis sans modification).
        if (role != previous) {
            agent.setSuperviseur(null);
        }
        // Un superviseur ou un chef d'équipe qui change de rôle n'encadre plus personne.
        if ((previous == AgentRole.SUPERVISEUR || previous == AgentRole.CHEF_EQUIPE) && role != previous) {
            agentRepository.findBySuperviseur_Id(id).forEach(a -> {
                a.setSuperviseur(null);
                agentRepository.save(a);
            });
        }
        return agentRepository.save(agent);
    }

    // Seuls les comptes AGENT et SUPERVISEUR sont affectés à une zone ; zone == null retire
    // l'affectation.
    public Agent assignZone(UUID id, Zone zone) {
        Agent agent = getAgent(id);
        if (zone != null && !agent.getRole().canHaveZone()) {
            throw new IllegalArgumentException("Seuls les agents et les superviseurs peuvent être affectés à une zone");
        }
        agent.setZone(zone);
        return agentRepository.save(agent);
    }

    public Agent resetPassword(UUID id, String newPassword) {
        Agent agent = getAgent(id);
        agent.setPasswordHash(passwordEncoder.encode(newPassword));
        agent.setGeneratedPassword(newPassword);
        return agentRepository.save(agent);
    }

    public Agent changeOwnPassword(UUID id, String currentPassword, String newPassword) {
        Agent agent = getAgent(id);
        if (!passwordEncoder.matches(currentPassword, agent.getPasswordHash())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }
        agent.setPasswordHash(passwordEncoder.encode(newPassword));
        // Choisi par la personne elle-même, pas par un ADMIN : on ne le garde pas en clair (et le
        // dernier mot de passe généré automatiquement, affiché dans "Gérer", n'est plus valide —
        // on l'efface plutôt que de laisser un ADMIN croire qu'il fonctionne encore).
        agent.setGeneratedPassword(null);
        return agentRepository.save(agent);
    }

    // Suppression définitive (pas une désactivation) — réservée à l'ADMIN/SUPER_ADMIN, cf.
    // AgentAdminController#deleteAgent et SecurityConfig. Refusée si le compte a des ménages
    // enregistrés (orphelins sinon, ou simple échec sur la contrainte de clé étrangère) ou s'il
    // est le superviseur d'autres comptes (eux aussi orphelins de superviseur) : désactiver est
    // la bonne option dans ces cas, pas supprimer.
    public void deleteAgent(UUID id) {
        Agent agent = getAgent(id);
        if (householdRepository.existsByAgent_Id(id)) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer : ce compte a des ménages enregistrés. Désactivez-le plutôt.");
        }
        if (siteRepository.existsByAgent_Id(id)) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer : ce compte a des fiches de repérage de site. Désactivez-le plutôt.");
        }
        if (agentRepository.existsBySuperviseur_Id(id)) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer : ce compte encadre encore des agents. Réaffectez-les, puis désactivez-le plutôt.");
        }
        agentRepository.delete(agent);
    }

    public Agent updatePhoto(UUID id, byte[] photo, String contentType) {
        if (photo.length > MAX_PHOTO_SIZE) {
            throw new IllegalArgumentException("La photo dépasse la taille maximale autorisée (2 Mo)");
        }
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier doit être une image");
        }
        Agent agent = getAgent(id);
        agent.setPhoto(photo, contentType);
        return agentRepository.save(agent);
    }
}
