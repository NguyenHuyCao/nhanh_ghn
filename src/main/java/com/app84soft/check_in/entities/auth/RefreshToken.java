package com.app84soft.check_in.entities.auth;

import com.app84soft.check_in.entities.user.User;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.Instant;

@Getter @Setter
@Entity @Table(name="refresh_tokens",
        indexes = {@Index(name="idx_rt_user", columnList="user_id"),
                @Index(name="idx_rt_token", columnList="token", unique=true)})
public class RefreshToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false) private User user;
    @Column(nullable=false, unique=true, length=300) private String token; // random 256-bit base64
    @Column(nullable=false) private Instant expiresAt;
    private Instant revokedAt;
    private String replacedBy; // token mới khi rotate
    private String userAgent; private String ip;
    public boolean isActive() { return revokedAt==null && Instant.now().isBefore(expiresAt); }
}
