package com.onip.facm01.bootstrap;

import com.onip.facm01.agent.AgentRole;
import com.onip.facm01.agent.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AgentService agentService;
    private final String bootstrapAdminUsername;
    private final String bootstrapAdminPassword;
    private final String bootstrapAgentUsername;
    private final String bootstrapAgentPassword;

    public DataSeeder(
            AgentService agentService,
            @Value("${app.bootstrap-admin.username}") String bootstrapAdminUsername,
            @Value("${app.bootstrap-admin.password}") String bootstrapAdminPassword,
            @Value("${app.bootstrap-agent.username}") String bootstrapAgentUsername,
            @Value("${app.bootstrap-agent.password}") String bootstrapAgentPassword) {
        this.agentService = agentService;
        this.bootstrapAdminUsername = bootstrapAdminUsername;
        this.bootstrapAdminPassword = bootstrapAdminPassword;
        this.bootstrapAgentUsername = bootstrapAgentUsername;
        this.bootstrapAgentPassword = bootstrapAgentPassword;
    }

    @Override
    public void run(String... args) {
        agentService.ensureAgentExists(bootstrapAdminUsername, bootstrapAdminPassword, "Administrateur", AgentRole.ADMIN);
        agentService.ensureAgentExists(bootstrapAgentUsername, bootstrapAgentPassword, "Agent de test", AgentRole.AGENT);
        log.info("Comptes de test disponibles : {} / *** (ADMIN, tableau de bord) et {} / *** (AGENT, app terrain) — changez ces mots de passe en production",
                bootstrapAdminUsername, bootstrapAgentUsername);
    }
}
