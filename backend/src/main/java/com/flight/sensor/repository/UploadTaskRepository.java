package com.flight.sensor.repository;

import com.flight.sensor.entity.UploadTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadTaskRepository extends JpaRepository<UploadTask, Long> {

    Optional<UploadTask> findByTaskId(String taskId);

    List<UploadTask> findAllByOrderByCreatedAtDesc();

    List<UploadTask> findByStatusOrderByCreatedAtDesc(String status);
}
