package com.codequest.service;

import com.codequest.dto.InterviewDtos.DiaryRequest;
import com.codequest.dto.InterviewDtos.DiaryView;
import com.codequest.entity.InterviewDiaryEntry;
import com.codequest.exception.ApiException;
import com.codequest.repository.InterviewDiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Interview Diary: real questions YOU faced. The only "actually asked" content in the app, and it's yours. */
@Service
public class DiaryService {
    private final InterviewDiaryRepository diary;

    public DiaryService(InterviewDiaryRepository diary) {
        this.diary = diary;
    }

    @Transactional(readOnly = true)
    public List<DiaryView> list(Long userId) {
        return diary.findByUserIdOrderByInterviewDateDescIdDesc(userId).stream().map(DiaryService::view).toList();
    }

    @Transactional
    public DiaryView save(Long userId, Long id, DiaryRequest req) {
        InterviewDiaryEntry e = id == null ? new InterviewDiaryEntry() : diary.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Diary entry"));
        e.setUserId(userId);
        e.setCompany(req.company().trim());
        e.setRound(req.round().trim());
        e.setInterviewDate(req.interviewDate());
        e.setQuestion(req.question().trim());
        e.setMyAnswer(req.myAnswer());
        e.setBetterAnswer(req.betterAnswer());
        e.setTags(req.tags());
        return view(diary.save(e));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        diary.delete(diary.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Diary entry")));
    }

    static DiaryView view(InterviewDiaryEntry e) {
        return new DiaryView(e.getId(), e.getCompany(), e.getRound(), e.getInterviewDate(), e.getQuestion(), e.getMyAnswer(), e.getBetterAnswer(),
                e.getTags(), e.getCreatedAt());
    }
}
