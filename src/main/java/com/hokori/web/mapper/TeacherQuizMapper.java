package com.hokori.web.mapper;

import com.hokori.web.dto.quiz.*;
import com.hokori.web.entity.Option;
import com.hokori.web.entity.Question;
import com.hokori.web.entity.Quiz;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeacherQuizMapper {

    public QuizDto toDto(Quiz q){
        return new QuizDto(
                q.getId(),
                q.getLesson().getId(),
                q.getTitle(),
                q.getDescription(),
                q.getTotalQuestions(),
                q.getTimeLimitSec(),
                q.getPassScorePercent()
        );
    }

    public OptionDto toDto(Option o){
        return new OptionDto(
                o.getId(),
                o.getContent(),
                o.getIsCorrect(),
                o.getOrderIndex()
        );
    }

    public QuestionWithOptionsDto toDto(Question qu, List<Option> opts){
        return new QuestionWithOptionsDto(
                qu.getId(),
                qu.getContent(),
                qu.getExplanation(),
                qu.getQuestionType(),
                qu.getOrderIndex(),
                opts.stream().map(this::toDto).toList()
        );
    }
}
