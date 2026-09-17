package com.onip.facm01.security;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
                .authorities("ROLE_" + agent.getRole().name())
                .disabled(!agent.isActive())
                .build();
    }
}
