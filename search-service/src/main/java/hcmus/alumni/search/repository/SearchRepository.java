package hcmus.alumni.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hcmus.alumni.search.dto.ISearchDto;
import hcmus.alumni.search.model.UserModel;

public interface SearchRepository extends JpaRepository<UserModel, String> {

//	@Query("SELECT DISTINCT u " + 
//		   "FROM UserModel u " + 
//		   "LEFT JOIN u.statusId s " + 
//		   "LEFT JOIN u.faculty f " + 
//		   "WHERE (:statusId IS NULL OR s.id = :statusId) " + 
//		   "AND (:facultyId IS NULL OR f.id = :facultyId) " + 
//		   "AND (:beginningYear IS NULL OR u.beginningYear = :beginningYear) " + 
//		   "AND s.id != 4 " +
//		   "AND u.fullName LIKE %:fullName%")
//	Page<ISearchDto> searchUsers(String fullName, Integer statusId,
//			Integer facultyId, Integer beginningYehar, Pageable pageable);

	@Query("SELECT DISTINCT u " + 
		       "FROM UserModel u " + 
		       "LEFT JOIN u.faculty f " + 
		       "WHERE (:statusId IS NULL OR u.statusId = :statusId) " + 
		       "AND (:facultyId IS NULL OR f.id = :facultyId) " + 
		       "AND u.statusId != 4 " +
		       "AND u.fullName LIKE %:fullName%")
		Page<ISearchDto> searchUsers(String fullName, Integer statusId, Integer facultyId, Pageable pageable);

	Long getCountByStatusId(@Param("statusId") Integer statusId);

}