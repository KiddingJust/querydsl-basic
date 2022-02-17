package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})   //team은 양방향 연관관계이므로 제외
public class MemberDSL {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private TeamDSL team;

    public MemberDSL(String username){
        this(username, 0);
    }
    public MemberDSL(String username, int age){
        this(username, age, null);
    }
    public MemberDSL(String username, int age, TeamDSL team){
        this.username = username;
        this.age = age;
        //왜 changeTeam 하는지 체크
        if (team != null){
            chagneTeam(team);
        }
    }
    private void chagneTeam(TeamDSL team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
