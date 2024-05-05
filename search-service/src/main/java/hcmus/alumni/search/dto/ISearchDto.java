package hcmus.alumni.search.dto;

public interface ISearchDto {
    interface User {
        String getId(); // Added
        String getFullName();
    }

    interface Faculty {
        Integer getId();
        String getName();
    }

    String getId();
    String getFullName();
    String getThumbnail();
    Faculty getFaculty();
    Integer getBeginningYear();
    
}