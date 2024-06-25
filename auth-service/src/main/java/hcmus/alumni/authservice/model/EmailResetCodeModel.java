package hcmus.alumni.authservice.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;

@Entity
@Table(name = "email_reset_code")
@Data
@AllArgsConstructor
public class EmailResetCodeModel implements Serializable {

	private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "email", nullable = false, length = 200)
    private String email;


	@Column(name = "reset_code", nullable = false, length = 8)
    private String resetCode;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    public EmailResetCodeModel() {
		// TODO Auto-generated constructor stub
	}
}
