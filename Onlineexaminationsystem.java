
import java.io.*;
import java.util.*;

public class Onlineexaminationsystem {

    static class Question {
        String text;
        String[] options = new String[4];
        char key;
    }

    static class Candidate {
        String name;
        int correct, wrong, marks, total;
    }

    static class Answered {            
        int number;
        Question q;
        char chosen;
        int marks;
    }

    static final int RIGHT_MARKS = 4;
    static final int WRONG_MARKS = -1;

    static String examFile = "exam.txt";
    static String resultFile = "results.txt";

    static ArrayList<Question> questions = new ArrayList<>();    
    static ArrayList<Candidate> candidates = new ArrayList<>();  
    static Stack<Answered> reviewStack = new Stack<>();          
    static String reviewName = "";
    static Scanner sc = new Scanner(System.in);

    static String readLine(String prompt) {
        System.out.print(prompt);
        if (!sc.hasNextLine()) {                 
            System.out.println();
            saveResults();
            System.out.println("Input ended. Results saved. Goodbye!");
            System.exit(0);
        }
        return sc.nextLine().trim();
    }

    static void createDefaultExam() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(examFile))) {
            pw.println("What does CPU stand for?|Central Program Unit|Central Processing Unit|Computer Personal Unit|Control Process Unit|B");
            pw.println("Which one is an operating system?|Windows|Chrome|Java|MySQL|A");
            pw.println("What is the full form of RAM?|Read Access Memory|Run Access Memory|Random Access Memory|Rapid Active Memory|C");
            System.out.println("exam.txt not found - a default exam file was created.");
        } catch (IOException e) {
            System.out.println("Could not create " + examFile + ": " + e.getMessage());
        }
    }

    static void loadQuestions() {
        File f = new File(examFile);
        if (!f.exists()) createDefaultExam();
        try (BufferedReader br = new BufferedReader(new FileReader(examFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("\\|", -1);
                if (p.length != 6) continue;                       // skip bad line
                String key = p[5].trim().toUpperCase();
                if (key.length() != 1 || "ABCD".indexOf(key.charAt(0)) < 0) continue;
                Question q = new Question();
                q.text = p[0].trim();
                for (int i = 0; i < 4; i++) q.options[i] = p[i + 1].trim();
                q.key = key.charAt(0);
                questions.add(q);
            }
        } catch (IOException e) {
            System.out.println("Could not read " + examFile + ": " + e.getMessage());
        }
    }

    static void loadResults() {
        File f = new File(resultFile);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.trim().split("\\|");
                if (p.length != 5) continue;
                try {
                    Candidate c = new Candidate();
                    c.name = p[0].trim();
                    c.correct = Integer.parseInt(p[1].trim());
                    c.wrong = Integer.parseInt(p[2].trim());
                    c.marks = Integer.parseInt(p[3].trim());
                    c.total = Integer.parseInt(p[4].trim());
                    candidates.add(c);
                } catch (NumberFormatException ignored) { }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + resultFile + ": " + e.getMessage());
        }
    }

    static void saveResults() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(resultFile))) {
            for (Candidate c : candidates)
                pw.println(c.name + "|" + c.correct + "|" + c.wrong + "|" + c.marks + "|" + c.total);
        } catch (IOException e) {
            System.out.println("Could not save " + resultFile + ": " + e.getMessage());
        }
    }

    static char readAnswer() {
        while (true) {
            String v = readLine("Your answer (A/B/C/D/S to skip): ").toUpperCase();
            if (v.length() == 1 && "ABCDS".indexOf(v.charAt(0)) >= 0) return v.charAt(0);
            System.out.println("Invalid option. Enter A, B, C, D or S (skip).");
        }
    }

    static void showQuestion(int number, Question q) {
        System.out.println("\nQ" + number + ". " + q.text);
        for (int i = 0; i < 4; i++)
            System.out.println("   " + (char) ('A' + i) + ") " + q.options[i]);
    }

    static void startExam() {
        if (questions.isEmpty()) {
            System.out.println("No questions available in " + examFile + ".");
            return;
        }
        String name = "";
        while (name.isEmpty()) {
            name = readLine("Enter candidate name: ").replace("|", " ").trim();
            if (name.isEmpty()) System.out.println("Name cannot be empty.");
        }

        Queue<Question> queue = new LinkedList<>(questions);   
        reviewStack.clear();
        reviewName = name;
        int correct = 0, wrong = 0, marks = 0;

        while (!queue.isEmpty()) {
            Question q = queue.poll();
            int number = questions.indexOf(q) + 1;
            showQuestion(number, q);
            char ans = readAnswer();

            if (ans == 'S') {                                  
                queue.add(q);
                System.out.println("Q" + number + " skipped and moved to end of queue.");
                continue;
            }
            Answered a = new Answered();
            a.number = number; a.q = q; a.chosen = ans;
            if (ans == q.key) { correct++; a.marks = RIGHT_MARKS; }
            else              { wrong++;   a.marks = WRONG_MARKS; }
            marks += a.marks;
            reviewStack.push(a);                               
        }

        Candidate c = new Candidate();
        c.name = name; c.correct = correct; c.wrong = wrong;
        c.marks = marks; c.total = questions.size() * RIGHT_MARKS;
        candidates.add(c);
        saveResults();

        System.out.println("\nExam submitted. " + name + " | Correct: " + correct
                + " | Wrong: " + wrong + " | Marks: " + marks + "/" + c.total);
    }

    @SuppressWarnings("unchecked")
    static void reviewExam() {
        if (reviewStack.empty()) {
            System.out.println("Nothing to review. Start an exam first.");
            return;
        }
        System.out.println("\n--- REVIEW: " + reviewName + " (last answered first) ---");
        Stack<Answered> copy = (Stack<Answered>) reviewStack.clone();
        while (!copy.empty()) {
            Answered a = copy.pop();
            System.out.println("Q" + a.number + ": " + a.q.text);
            System.out.println("    Your answer: " + a.chosen + " | Correct answer: " + a.q.key
                    + " | Marks: " + (a.marks > 0 ? "+" : "") + a.marks);
        }
    }


    static void showRankList() {
        if (candidates.isEmpty()) {
            System.out.println("No results yet.");
            return;
        }
        ArrayList<Candidate> sorted = new ArrayList<>(candidates);
        Collections.sort(sorted, new Comparator<Candidate>() {      
            public int compare(Candidate a, Candidate b) {
                if (b.marks != a.marks) return Integer.compare(b.marks, a.marks);
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        System.out.println("\n--- RANK LIST ---");
        int rank = 1;
        for (Candidate c : sorted)
            System.out.println(rank++ + ". " + c.name + " | " + c.marks);
    }


    static int readMenu() {
        while (true) {
            String v = readLine("Enter your choice: ");
            try {
                int n = Integer.parseInt(v);
                if (n >= 0 && n <= 3) return n;
                System.out.println("Invalid choice. Please enter 0-3.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    static void showMenu() {
        System.out.println("\n==== ONLINE EXAMINATION SYSTEM ====");
        System.out.println("1. Start Exam");
        System.out.println("2. Review Last Exam");
        System.out.println("3. Rank List");
        System.out.println("0. Exit (save results)");
    }

    public static void main(String[] args) {
        if (args.length > 0) examFile = args[0];
        if (args.length > 1) resultFile = args[1];
        loadQuestions();
        loadResults();

        while (true) {
            showMenu();
            int choice = readMenu();
            switch (choice) {
                case 1: startExam(); break;
                case 2: reviewExam(); break;
                case 3: showRankList(); break;
                case 0:
                    saveResults();
                    System.out.println("Results saved to " + resultFile + ". Thank you!");
                    sc.close();
                    return;
                default: break;
            }
        }
    }
}