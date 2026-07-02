package com.example.tsubuyaki.repository;

import com.example.tsubuyaki.domain.PostTag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    @Query("""
            SELECT pt
            FROM PostTag pt
            JOIN FETCH pt.post p
            WHERE pt.tag.name = :name
            AND p.deletedAt IS NULL
            ORDER BY p.createdAt DESC
            """)
    List<PostTag> findTop50ByTagNameOrderByPostCreatedAtDesc(@Param("name") String name, Pageable pageable);

    @Query("""
            SELECT pt
            FROM PostTag pt
            JOIN FETCH pt.post p
            WHERE pt.tag.name = :name
            AND p.deletedAt IS NULL
            ORDER BY (
                SELECT COUNT(pl.id)
                FROM PostLike pl
                WHERE pl.post = p
            ) DESC, p.createdAt DESC
            """)
    List<PostTag> findByTagNameOrderByLikeCountDescCreatedAtDesc(@Param("name") String name, Pageable pageable);
}
