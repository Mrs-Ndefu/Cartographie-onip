package com.onip.facm01.security;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.AgentRole;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgentUserDetailsService implements UserDetailsService {

    private final AgentRepository agentRepository;

    public AgentUserDetailsService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Agent agent = agentRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Agent introuvable : " + username));

        return User.withUsername(agent.getUsername())
                .password(agent.getPasswordHash())
                .authorities(authorities(agent.getRole()).toArray(new String[0]))
                .disabled(!agent.isActive())
                .build();
    }

    // Un rôle = une autorité. Le SUPER_ADMIN ne reçoit plus ROLE_ADMIN : il voit tout mais ne
    // pose pas d'action sur les ménages, donc les règles d'accès le nomment explicitement là où
    // il est autorisé (cf. SecurityConfig).
    private List<String> authorities(AgentRole role) {
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_" + role.name());
        return authorities;
    }
}
