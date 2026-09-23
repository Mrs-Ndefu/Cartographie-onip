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

    // Un SUPER_ADMIN reçoit aussi ROLE_ADMIN : il doit accéder à tout ce qu'un ADMIN peut
    // (tableau de bord, endpoints @PreAuthorize("hasRole('ADMIN')")) en plus de ses droits propres.
    private List<String> authorities(AgentRole role) {
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_" + role.name());
        if (role == AgentRole.SUPER_ADMIN) {
            authorities.add("ROLE_ADMIN");
        }
        return authorities;
    }
}
