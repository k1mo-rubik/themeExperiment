package ru.lukianenko.themeexperiment.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.lukianenko.themeexperiment.dto.Feedback;
import ru.lukianenko.themeexperiment.dto.Question;
import ru.lukianenko.themeexperiment.dto.TextEntity;
import ru.lukianenko.themeexperiment.dto.UserDto;
import ru.lukianenko.themeexperiment.dto.UserTextResult;
import ru.lukianenko.themeexperiment.repo.FeedbackRepository;
import ru.lukianenko.themeexperiment.repo.QuestionRepository;
import ru.lukianenko.themeexperiment.repo.TextRepository;
import ru.lukianenko.themeexperiment.repo.UserRepository;
import ru.lukianenko.themeexperiment.repo.UserTextResultRepository;

import java.util.List;

@Controller
public class ExperimentController {

    @Autowired
    private UserRepository userRepo;
    @Autowired
    private TextRepository textRepo;
    @Autowired
    private QuestionRepository questionRepo;
    @Autowired
    private UserTextResultRepository userTextResultRepo;
    @Autowired
    private FeedbackRepository feedbackRepo;

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @PostMapping("/start")
    public String start(
        @RequestParam String nickname,
        @RequestParam String gender,
        @RequestParam Integer age
    ) {
        UserDto userDto = new UserDto();
        userDto.setNickname(nickname);
        userDto.setGender(gender);
        userDto.setAge(age);

        long count = userRepo.count();
        // Например, если count четное, А светлая, иначе А темная
        boolean conditionAIsLight = (count % 2 == 0);
        userDto.setConditionAIsLight(conditionAIsLight);

        userDto = userRepo.save(userDto);

        // Сохраняем userId в сессию
        return "redirect:/experiment/1?userId=" + userDto.getId();
    }

    @GetMapping("/experiment/{textNumber}")
    public String showText(
        @PathVariable int textNumber,
        @RequestParam Long userId,
        Model model
    ) {
        UserDto userDto = userRepo.findById(userId).orElseThrow();
        
        // Определяем текст по номеру
        // Предположим texts: id=1,2 -> A; id=3,4 -> B
        // или же мы можем заранее зашить соответствие textNumber -> textId
        Long textId = (long) textNumber;

        TextEntity textEntity = textRepo.findById(textId).orElseThrow();
        
        boolean isGroupA = textEntity.getGroupName().equals("A");
        boolean conditionAIsLight = userDto.isConditionAIsLight();
        
        String themeClass;
        if (isGroupA) {
            themeClass = conditionAIsLight ? "light-theme" : "dark-theme";
        } else {
            themeClass = conditionAIsLight ? "dark-theme" : "light-theme";
        }

        model.addAttribute("themeClass", themeClass);
        model.addAttribute("textNumber", textNumber);
        model.addAttribute("textContent", textEntity.getContent());
        model.addAttribute("startTime", System.currentTimeMillis());
        model.addAttribute("userId", userId);

        return "experiment";
    }

    @PostMapping("/experiment/{textNumber}/next")
    public String nextText(
        @PathVariable int textNumber,
        @RequestParam Long userId,
        @RequestParam long startTime
    ) {
        long endTime = System.currentTimeMillis();
        long readingTime = endTime - startTime;

        // Создадим/обновим запись UserTextResult (пока без ответа на вопросы)
        UserTextResult utr = new UserTextResult();
        utr.setUserId(userId);
        utr.setTextId((long)textNumber);
        utr.setReadingTimeMillis(readingTime);
        utr = userTextResultRepo.save(utr);

        return "redirect:/experiment/" + textNumber + "/quiz?userId=" + userId;
    }

    @GetMapping("/experiment/{textNumber}/quiz")
    public String showQuiz(
        @PathVariable int textNumber,
        @RequestParam Long userId,
        Model model
    ) {
        List<Question> questions = questionRepo.findByTextId((long) textNumber);
        model.addAttribute("questions", questions);
        model.addAttribute("textNumber", textNumber);
        model.addAttribute("userId", userId);
        return "quiz";
    }

    @PostMapping("/experiment/{textNumber}/quiz")
    public String submitQuiz(
        @PathVariable int textNumber,
        @RequestParam Long userId,
        HttpServletRequest request
    ) {
        List<Question> questions = questionRepo.findByTextId((long) textNumber);
        int correctCount = 0;
        for (Question q : questions) {
            String answer = request.getParameter("q" + q.getId());
            if (answer != null) {
                int ansVal = Integer.parseInt(answer);
                if (ansVal == q.getCorrectOptionIndex()) {
                    correctCount++;
                }
            }
        }

        // Обновляем запись в UserTextResult с количеством правильных ответов
        UserTextResult utr = userTextResultRepo.findByUserIdAndTextId(userId, (long)textNumber);
        utr.setCorrectAnswersCount(correctCount);
        userTextResultRepo.save(utr);

        if (textNumber < 4) {
            return "redirect:/experiment/" + (textNumber+1) + "?userId=" + userId;
        } else {
            return "redirect:/feedback?userId=" + userId;
        }
    }

    @GetMapping("/feedback")
    public String feedback(
        @RequestParam Long userId,
        Model model
    ) {
        model.addAttribute("userId", userId);
        return "feedback";
    }

    @PostMapping("/feedback")
    public String submitFeedback(
            @RequestParam String feedbackText,
            @RequestParam Long userId) {
        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setFeedbackText(feedbackText);
        feedbackRepo.save(fb);
        return "thankyou";
    }
}
