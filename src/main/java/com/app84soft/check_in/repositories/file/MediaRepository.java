package com.app84soft.check_in.repositories.file;

import com.app84soft.check_in.entities.file.UploadFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<UploadFile, Integer> {
    UploadFile findUploadFileById(int id);

    List<UploadFile> findByName(String name);
}
