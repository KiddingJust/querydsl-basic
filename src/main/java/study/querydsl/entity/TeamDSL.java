package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter  @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  //JPA는 기본생성자 필요
@ToString(of = {"id", "name"})   //team은 양방향 연관관계이므로 제외
public class TeamDSL {

    @Id @GeneratedValue
    @Column(name="team_id")
    private Long id;
    private String name;

    @OneToMany(mappedBy="team")
    private List<MemberDSL> members = new ArrayList<>();

    public TeamDSL(String name){
        this.name = name;
    }
}
