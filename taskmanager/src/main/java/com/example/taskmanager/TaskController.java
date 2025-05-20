package com.example.taskmanager;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

@Controller
public class TaskController {

    @Autowired
    private TaskRepository taskRepo;

    @GetMapping("/tasks")
    public String listTasks(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Task> tasks = (keyword != null && !keyword.isEmpty())
                ? taskRepo.findByTitleContainingIgnoreCase(keyword)
                : taskRepo.findAll();

        model.addAttribute("tasks", tasks);
        model.addAttribute("task", new Task());
        model.addAttribute("keyword", keyword);
        return "tasks";
    }

    @PostMapping("/tasks")
    public String saveTask(@ModelAttribute Task task) {
        if (task.getDeadlineDate() == null) task.setDeadlineDate(LocalDate.now()); // Default to today if not set
        taskRepo.save(task);
        return "redirect:/tasks";
    }

    @GetMapping("/tasks/delete/{id}")
    public String deleteTask(@PathVariable Long id) {
        taskRepo.deleteById(id);
        return "redirect:/tasks";
    }

    @GetMapping("/tasks/edit/{id}")
    public String editTask(@PathVariable Long id, Model model) {
        model.addAttribute("task", taskRepo.findById(id).orElse(new Task()));
        model.addAttribute("tasks", taskRepo.findAll());
        return "tasks";
    }

    @PostMapping("/tasks/toggle/{id}")
    public String toggleStatus(@PathVariable Long id) {
        Task task = taskRepo.findById(id).orElse(null);
        if (task != null) {
            task.setCompleted(!task.isCompleted());
            taskRepo.save(task);
        }
        return "redirect:/tasks";
    }

    @GetMapping("/tasks/export/pdf")
    public void exportToPDF(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=tasks.pdf");

        List<Task> tasks = taskRepo.findAll();
        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        document.add(new Paragraph("📋 Task List\n\n"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Task task : tasks) {
            String deadline = (task.getDeadlineDate() != null && task.getDeadlineTime() != null)
                    ? task.getDeadlineDate().atTime(task.getDeadlineTime()).format(formatter)
                    : "No Deadline";
            document.add(new Paragraph(
                    String.format("- %s [%s] | Deadline: %s", task.getTitle(), task.isCompleted() ? "✅ Completed" : "⏳ Not Completed", deadline)
            ));
        }

        document.close();
    }
}