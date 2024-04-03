package hcmus.alumni.news.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import hcmus.alumni.news.dto.INewsDto;
import hcmus.alumni.news.dto.NewsDto;
import hcmus.alumni.news.model.NewsModel;
import hcmus.alumni.news.model.StatusPostModel;
import hcmus.alumni.news.model.UserModel;
import hcmus.alumni.news.repository.NewsRepository;
import hcmus.alumni.news.utils.ImageUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/news")
public class NewsServiceController {
	@PersistenceContext
	private EntityManager em;

	@Autowired
	private NewsRepository newsRepository;
	@Autowired
	private ImageUtils imageUtils;

	@GetMapping("/count")
	public ResponseEntity<Long> getPendingAlumniVerificationCount(@RequestParam(value = "status") String status) {
		if (status.equals("")) {
			ResponseEntity.status(HttpStatus.OK).body(0);
		}
		return ResponseEntity.status(HttpStatus.OK).body(newsRepository.getCountByStatus(status));
	}

	@GetMapping("")
	public ResponseEntity<HashMap<String, Object>> getNews(
			@RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
			@RequestParam(value = "limit", required = false, defaultValue = "10") int limit,
			@RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
			@RequestParam(value = "createAtOrder", required = false, defaultValue = "desc") String createAtOrder) {
		// Initiate
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<NewsDto> cq = cb.createQuery(NewsDto.class);
		// From
		Root<NewsModel> root = cq.from(NewsModel.class);

		// Join
//		Join<NewsModel, UserModel> userJoin = root.join("user", JoinType.INNER);
		Join<NewsModel, StatusPostModel> statusJoin = root.join("status", JoinType.INNER);

		// Select
		Selection<String> idSelection = root.get("id");
		Selection<String> titleSelection = root.get("title");
		Selection<String> contentSelection = root.get("content");
		Selection<String> thumbnailSelection = root.get("thumbnail");
		Selection<Integer> viewsSelection = root.get("views");
		cq.multiselect(idSelection, titleSelection, contentSelection, thumbnailSelection, viewsSelection);

		// Where
		Predicate titlePredicate = cb.like(root.get("title"), "%" + keyword + "%");
		Predicate statusPredicate = cb.equal(statusJoin.get("name"), "Bình thường");
		cq.where(titlePredicate, statusPredicate);

		// Order by
		List<Order> orderList = new ArrayList<Order>();
		if (createAtOrder.equals("asc")) {
			orderList.add(cb.asc(root.get("createAt")));
		} else if (createAtOrder.equals("desc")) {
			orderList.add(cb.desc(root.get("createAt")));
		}
		cq.orderBy(orderList);

		HashMap<String, Object> result = new HashMap<String, Object>();
		TypedQuery<NewsDto> typedQuery = em.createQuery(cq);
		result.put("itemNumber", typedQuery.getResultList().size());
		typedQuery.setFirstResult(offset);
		typedQuery.setMaxResults(limit);
		result.put("items", typedQuery.getResultList());

		return ResponseEntity.status(HttpStatus.OK).body(result);
	}

	@GetMapping("/{id}")
	public ResponseEntity<INewsDto> getNewsById(@PathVariable String id) {
		Optional<INewsDto> optionalNews = newsRepository.findNewsById(id);
		if (optionalNews.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}
		return ResponseEntity.status(HttpStatus.OK).body(optionalNews.get());
	}

	@PreAuthorize("hasAnyAuthority('Admin')")
	@PostMapping("")
	public ResponseEntity<String> createNews(@RequestHeader("userId") String creator,
			@RequestParam(value = "title") String title, @RequestParam(value = "content") String content,
			@RequestParam(value = "thumbnail") MultipartFile thumbnail) {
		if (creator.isEmpty() || title.isEmpty() || content.isEmpty() || thumbnail.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("All fields must not be empty");
		}
		if (thumbnail.getSize() > 5 * 1024 * 1024) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File must be lower than 5MB");
		}
		String id = UUID.randomUUID().toString();

		String processedContent = imageUtils.saveImgFromHtmlToStorage(content, id);

		try {
			// Save thumbnail image
			String thumbnailUrl = imageUtils.saveImageToStorage(imageUtils.getNewsPath(id), thumbnail, "thumbnail");
			// Save news to database
			NewsModel news = new NewsModel(id, new UserModel(creator), title, processedContent, thumbnailUrl);
			newsRepository.save(news);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println(e);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body("");
	}

	@PreAuthorize("hasAnyAuthority('Admin')")
	@PutMapping("/{id}")
	public ResponseEntity<String> updateNews(@PathVariable String id,
			@RequestParam(value = "title", defaultValue = "") String title,
			@RequestParam(value = "content", defaultValue = "") String content,
			@RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {
		if (thumbnail != null && thumbnail.getSize() > 5 * 1024 * 1024) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File must be lower than 5MB");
		}

		try {
			// Find news
			Optional<NewsModel> optionalNews = newsRepository.findById(id);
			if (optionalNews.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid id");
			}
			NewsModel news = optionalNews.get();
			if (!title.equals("")) {
				news.setTitle(title);
			}
			if (!content.equals("")) {
				String newContent= imageUtils.updateImgFromHtmlToStorage(news.getContent(), content, id);
				news.setContent(newContent);
			}
			if (thumbnail != null && !thumbnail.isEmpty()) {
				// Overwrite old thumbnail
				imageUtils.saveImageToStorage(imageUtils.getNewsPath(id), thumbnail, "thumbnail");
			}
			if (title != null | content != null)
				newsRepository.save(news);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println(e);
		}
		return ResponseEntity.status(HttpStatus.OK).body("");
	}
}
