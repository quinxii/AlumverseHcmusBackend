package hcmus.alumni.counsel.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import hcmus.alumni.counsel.dto.IPostAdviseDto;
import hcmus.alumni.counsel.model.PostAdviseModel;

public interface PostAdviseRepository extends JpaRepository<PostAdviseModel, String> {
	Optional<PostAdviseModel> findById(String id);

	@Query("SELECT pa FROM PostAdviseModel pa JOIN pa.status s WHERE s.id != 4 AND pa.id = :id AND pa.creator.id = :creatorId")
	Optional<PostAdviseModel> findByIdAndCreator(@Param("id") String id, @Param("creatorId") String creatorId);

	@Query("SELECT pa FROM PostAdviseModel pa JOIN pa.status s WHERE s.id = 2 AND pa.id = :id")
	Optional<IPostAdviseDto> findPostAdviseById(String id);

	@Query("SELECT DISTINCT new PostAdviseModel(pa, ipa.isDelete, :userId) " +
			"FROM PostAdviseModel pa " +
			"JOIN pa.status s " +
			"LEFT JOIN pa.tags t " +
			"LEFT JOIN InteractPostAdviseModel ipa ON pa.id = ipa.id.postAdviseId AND ipa.id.creator = :userId " +
			"WHERE (:tagsId IS NULL OR t.id IN :tagsId) " +
			"AND s.id = 2 " +
			"AND (:title IS NULL OR pa.title like %:title%)")
	Page<IPostAdviseDto> searchPostAdvise(String title, String userId, List<Integer> tagsId, Pageable pageable);

	@Query("SELECT pa FROM PostAdviseModel pa JOIN pa.status s WHERE s.id = 2 AND pa.publishedAt >= :startDate AND pa.publishedAt <= :endDate")
	Page<IPostAdviseDto> getHotNews(Date startDate, Date endDate, Pageable pageable);

	@Query("SELECT COUNT(pa) FROM PostAdviseModel pa JOIN pa.status s WHERE s.id = :statusId")
	Long getCountByStatusId(@Param("statusId") Integer statusId);

	@Query("SELECT COUNT(pa) FROM PostAdviseModel pa JOIN pa.status s WHERE s.id != 4")
	Long getCountByNotDelete();

	@Query("SELECT pa from PostAdviseModel pa JOIN pa.status s WHERE s.id = 1 AND pa.publishedAt <= :now")
	List<PostAdviseModel> getScheduledNews(Date now);

	@Transactional
	@Modifying
	@Query("UPDATE PostAdviseModel pa SET pa.childrenCommentNumber = pa.childrenCommentNumber + :count WHERE pa.id = :id")
	int commentCountIncrement(String id, @Param("count") Integer count);

	@Transactional
	@Modifying
	@Query("UPDATE PostAdviseModel pa SET pa.reactionCount = pa.reactionCount + :count WHERE pa.id = :id")
	int reactionCountIncrement(String id, @Param("count") Integer count);
}