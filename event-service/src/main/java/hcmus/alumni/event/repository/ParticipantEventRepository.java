package hcmus.alumni.event.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import hcmus.alumni.event.dto.IParticipantEventDto;
import hcmus.alumni.event.model.ParticipantEventModel;

public interface ParticipantEventRepository extends JpaRepository<ParticipantEventModel, String> {
	@Query("SELECT pe.note AS note, " +
		"u.fullName AS fullName, " +
		"u.avatarUrl AS avatarUrl " +
		"FROM ParticipantEventModel pe " +
		"JOIN UserModel u ON pe.id.userId = u.id " +
		"WHERE pe.id.eventId = :id " +
		"AND pe.isDelete = false")
	List<IParticipantEventDto> getParticipantsByEventId(@Param("id") String id);

	@Transactional
	@Modifying
	@Query("UPDATE ParticipantEventModel pe SET pe.isDelete = true WHERE pe.id.eventId = :eventId AND pe.id.userId = :userId")
	int deleteByEventIdAndUserId(@Param("eventId") String eventId, @Param("userId") String userId);
}
