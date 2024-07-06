package hcmus.alumni.userservice.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import hcmus.alumni.userservice.dto.IFriendDto;
import hcmus.alumni.userservice.model.FriendId;
import hcmus.alumni.userservice.model.FriendModel;
import hcmus.alumni.userservice.model.FriendRequestModel;

public interface FriendRepository extends JpaRepository<FriendModel, FriendId> {

	@Query("SELECT f FROM FriendModel f WHERE f.id.userId = :userId AND f.isDelete = false")
	Page<IFriendDto> getAllUserFriends(String userId, Pageable pageable);

	@Query(value = "select count(*) from friend f "
			+ "where f.user_id = :userId and f.is_delete = false", nativeQuery = true)
	Long countFriendByUserId(String userId);

	@Query("SELECT DISTINCT f FROM FriendModel f JOIN UserModel u ON f.id.friendId = u.id WHERE f.id.userId = :userId AND f.isDelete = false AND (:fullName IS NULL OR u.fullName LIKE %:fullName%)")
	Page<IFriendDto> getUserFriendsByFullName(String userId, String fullName, Pageable pageable);

	@Query("SELECT f FROM FriendModel f WHERE f.id.userId = :userId AND f.id.friendId = :friendId AND f.isDelete = false")
	Optional<FriendModel> findByUserIdAndFriendIdAndIsDelete(String userId, String friendId);
	
}