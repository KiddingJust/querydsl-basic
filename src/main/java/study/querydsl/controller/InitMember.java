package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.MemberDSL;
import study.querydsl.entity.TeamDSL;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {

        @PersistenceContext
        EntityManager em;

        @Transactional
        public void init() {
            TeamDSL teamA = new TeamDSL("teamA");
            TeamDSL teamB = new TeamDSL("teamB");
            em.persist(teamA);
            em.persist(teamB);
            for (int i = 0; i < 100; i++) {
                TeamDSL selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new MemberDSL("member" + i, i, selectedTeam));
            }
        }
    }
}