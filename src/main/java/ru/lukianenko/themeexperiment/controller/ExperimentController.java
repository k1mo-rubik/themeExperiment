package ru.lukianenko.themeexperiment.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.lukianenko.themeexperiment.dto.Feedback;
import ru.lukianenko.themeexperiment.dto.Question;
import ru.lukianenko.themeexperiment.dto.TextEntity;
import ru.lukianenko.themeexperiment.dto.UserDto;
import ru.lukianenko.themeexperiment.dto.UserTextResult;
import ru.lukianenko.themeexperiment.repo.FeedbackRepository;
import ru.lukianenko.themeexperiment.repo.QuestionRepository;
import ru.lukianenko.themeexperiment.repo.TextRepository;
import ru.lukianenko.themeexperiment.repo.UserDtoRepository;
import ru.lukianenko.themeexperiment.repo.UserTextResultRepository;

import java.util.*;

@Controller
public class ExperimentController {

    @Autowired
    private UserDtoRepository userRepo;
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
        userDto.setId(UUID.randomUUID()); // Генерация UUID
        userDto.setNickname(nickname);
        userDto.setGender(gender);
        userDto.setAge(age);

        long count = userRepo.count();
        boolean conditionAIsLight = (count % 2 == 0);
        userDto.setConditionAIsLight(conditionAIsLight);

        userDto = userRepo.save(userDto);

        return "redirect:/experiment/1?userId=" + userDto.getId();
    }

    @GetMapping("/experiment/{textNumber}")
    public String showText(
            @PathVariable int textNumber,
            @RequestParam UUID userId,
            Model model
    ) {
        UserDto userDto = userRepo.findById(userId).orElseThrow();
        TextEntity textEntity = textRepo.findById((long) textNumber).orElseThrow();

        boolean isGroupA = "A".equals(textEntity.getGroupName());
        boolean conditionAIsLight = userDto.isConditionAIsLight();

        String themeClass = isGroupA == conditionAIsLight ? "light-theme" : "dark-theme";

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
            @RequestParam UUID userId,
            @RequestParam long startTime
    ) {
        long endTime = System.currentTimeMillis();
        long readingTime = endTime - startTime;

        UserDto userDto = userRepo.findById(userId).orElseThrow();
        TextEntity textEntity = textRepo.findById((long) textNumber).orElseThrow();

        UserTextResult utr = new UserTextResult();
        utr.setUserId(userId);
        utr.setTextId((long)textNumber);
        utr.setReadingTimeMillis(readingTime);

        boolean isGroupA = "A".equals(textEntity.getGroupName());
        boolean conditionAIsLight = userDto.isConditionAIsLight();
        utr.setDarkTheme(isGroupA != conditionAIsLight);

        userTextResultRepo.save(utr);

        return "redirect:/experiment/" + textNumber + "/quiz?userId=" + userId;
    }

    @GetMapping("/experiment/{textNumber}/quiz")
    public String showQuiz(
            @PathVariable int textNumber,
            @RequestParam UUID userId,
            Model model
    ) {
        UserDto userDto = userRepo.findById(userId).orElseThrow();
        TextEntity textEntity = textRepo.findById((long) textNumber).orElseThrow();

        boolean isGroupA = textEntity.getGroupName().equals("A");
        boolean conditionAIsLight = userDto.isConditionAIsLight();
        String themeClass = isGroupA == conditionAIsLight ? "light-theme" : "dark-theme";

        model.addAttribute("themeClass", themeClass);
        model.addAttribute("questions", questionRepo.findByTextId((long) textNumber));
        model.addAttribute("textNumber", textNumber);
        model.addAttribute("userId", userId);

        return "quiz";
    }

    @PostMapping("/experiment/{textNumber}/quiz")
    public String submitQuiz(
            @PathVariable int textNumber,
            @RequestParam UUID userId,
            HttpServletRequest request
    ) {
        List<Question> questions = questionRepo.findByTextId((long) textNumber);
        int correctCount = 0;
        for (Question q : questions) {
            String answer = request.getParameter("q" + q.getId());
            if (answer != null && Integer.parseInt(answer) == q.getCorrectOptionIndex()) {
                correctCount++;
            }
        }

        UserTextResult utr = userTextResultRepo.findByUserIdAndTextId(userId, (long) textNumber);
        utr.setCorrectAnswersCount(correctCount);
        userTextResultRepo.save(utr);

        if (textNumber < 4) {
            return textNumber == 2
                    ? "redirect:/experiment/" + (textNumber + 1) + "/transition?userId=" + userId
                    : "redirect:/experiment/" + (textNumber + 1) + "?userId=" + userId;
        } else {
            return "redirect:/feedback?userId=" + userId;
        }
    }

    @GetMapping("/experiment/{textNumber}/transition")
    public String transitionPage(
            @PathVariable int textNumber,
            @RequestParam UUID userId,
            Model model
    ) {
        boolean isSwap = textNumber == 3;
        model.addAttribute("isSwap", isSwap);
        model.addAttribute("userId", userId);
        model.addAttribute("textNumber", textNumber);
        return "transition";
    }

    @GetMapping("/feedback")
    public String feedback(
            @RequestParam UUID userId,
            Model model
    ) {
        model.addAttribute("userId", userId);
        return "feedback";
    }

    @PostMapping("/feedback")
    public String submitFeedback(
            @RequestParam String feedbackText,
            @RequestParam UUID userId,
            @RequestParam Boolean prefersDarkTheme
    ) {
        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setFeedbackText(feedbackText);
        fb.setPrefersDarkTheme(prefersDarkTheme);
        feedbackRepo.save(fb);

        return "thankyou";
    }

    @GetMapping("/api/adminold")
    public String adminOldPage(Model model) {
        List<UserDto> users = userRepo.findAll();
        List<UserTextResult> results = userTextResultRepo.findAll();

        Map<UUID, UserDto> userMap = new HashMap<>();
        for (UserDto u : users) {
            userMap.put(u.getId(), u);
        }

        model.addAttribute("userMap", userMap);
        model.addAttribute("results", results);

        return "adminold";
    }
}
