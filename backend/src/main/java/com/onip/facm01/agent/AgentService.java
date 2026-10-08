package com.onip.facm01.agent;

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
    private final PasswordEncoder passwordEncoder;

    public AgentService(
            AgentRepository agentRepository, StaffMemberRepository staffMemberRepository, PasswordEncoder passwordEncoder) {
        this.agentRepository = agentRepository;
        this.staffMemberRepository = staffMemberRepository;
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
        return agentRepository.save(agent);
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

    public Page<Agent> listAgents(Pageable pageable) {
        return agentRepository.findAll(pageable);
    }

    // Agents affectés à un superviseur donné (la page "Gérer les agents" d'un SUPERVISEUR).
    public Page<Agent> listAgentsOf(UUID superviseurId, Pageable pageable) {
        return agentRepository.findBySuperviseur_Id(superviseurId, pageable);
    }

    public List<Agent> listSupervisors() {
        return agentRepository.findByRoleAndActiveTrueOrderByFullNameAsc(AgentRole.SUPERVISEUR);
    }

    // superviseur == null retire l'affectation.
    public Agent assignSupervisor(UUID id, Agent superviseur) {
        Agent agent = getAgent(id);
        if (agent.getRole() != AgentRole.AGENT) {
            throw new IllegalArgumentException("Seul un compte AGENT peut être affecté à un superviseur");
        }
        if (superviseur != null && superviseur.getRole() != AgentRole.SUPERVISEUR) {
            throw new IllegalArgumentException("Le compte choisi n'est pas un superviseur");
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
        if (role != AgentRole.AGENT) {
            agent.setSuperviseur(null);
        }
        // Un superviseur qui change de rôle n'encadre plus personne.
        if (previous == AgentRole.SUPERVISEUR && role != AgentRole.SUPERVISEUR) {
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
        return agentRepository.save(agent);
    }

    public Agent changeOwnPassword(UUID id, String currentPassword, String newPassword) {
        Agent agent = getAgent(id);
        if (!passwordEncoder.matches(currentPassword, agent.getPasswordHash())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }
        agent.setPasswordHash(passwordEncoder.encode(newPassword));
        return agentRepository.save(agent);
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
