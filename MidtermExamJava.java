public class MidtermExamJava {

    // ─── Student Class (used in Q4, Q5, Q8, Q9) ───────────────────────────────
    static class Student {
        String name;
        int score;

        Student(String name, int score) {
            this.name  = name;
            this.score = score;
        }

        @Override
        public String toString() {
            return name + ": " + score;
        }
    }

    // ─── Q2: Find Maximum Value ────────────────────────────────────────────────
    public static int findMax(int[] arr) {
        int max = arr[0];
        for (int val : arr) {
            if (val > max) max = val;
        }
        return max;
    }

    // ─── Q3: Add Bonus Points ─────────────────────────────────────────────────
    public static void addBonus(int[] scores) {
        for (int i = 0; i < scores.length; i++) {
            scores[i] += 5;
        }
    }

    // ─── Q5: Apply Score Curve ────────────────────────────────────────────────
    static void curve(Student s) {
        if (s.score < 60) {
            s.score += 10;
        }
    }

    // ─── Q7: Sum All Elements ─────────────────────────────────────────────────
    public static int sum(int[] arr) {
        int total = 0;
        for (int val : arr) {
            total += val;
        }
        return total;
    }

    // ─── Q9: Update Student's Score ────────────────────────────────────────────
    static void updateScore(Student s, int newScore) {
        s.score = newScore;
    }

    // ─── Main ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) {

        // ── Q1: Calculate the Average Score ──────────────────────────────────
        System.out.println("=== Q1: Calculate the Average Score ===");
        int[] scores = {70, 80, 90};
        double average = 0;
        for (int s : scores) average += s;
        average /= scores.length;
        System.out.println("Average: " + average);
        System.out.println();

        // ── Q2: Find the Maximum Value ────────────────────────────────────────
        System.out.println("=== Q2: Find the Maximum Value ===");
        int[] arr2 = {3, 9, 1, 7, 4};
        System.out.println(findMax(arr2));
        System.out.println();

        // ── Q3: Add Bonus Points to Each Score ───────────────────────────────
        System.out.println("=== Q3: Add Bonus Points to Each Score ===");
        int[] arr3 = {60, 70};
        addBonus(arr3);
        System.out.print("{");
        for (int i = 0; i < arr3.length; i++) {
            System.out.print(arr3[i]);
            if (i < arr3.length - 1) System.out.print(", ");
        }
        System.out.println("}");
        System.out.println();

        // ── Q4: Create and Print a Student Object ─────────────────────────────
        System.out.println("=== Q4: Create and Print a Student Object ===");
        Student tom = new Student("Tom", 85);
        System.out.println(tom);
        System.out.println();

        // ── Q5: Apply Score Curve to a Student ───────────────────────────────
        System.out.println("=== Q5: Apply Score Curve to a Student ===");
        Student tom5 = new Student("Tom", 55);
        curve(tom5);
        System.out.println(tom5);
        System.out.println();

        // ── Q6: Count Passing Students ────────────────────────────────────────
        System.out.println("=== Q6: Count Passing Students ===");
        int[] arr6 = {45, 60, 80, 59, 100};
        int passing = 0;
        for (int s : arr6) {
            if (s >= 60) passing++;
        }
        System.out.println("Number of passing students: " + passing);
        System.out.println();

        // ── Q7: Sum All Elements in an Array ─────────────────────────────────
        System.out.println("=== Q7: Sum All Elements in an Array ===");
        int[] arr7 = {1, 2, 3, 4};
        System.out.println(sum(arr7));
        System.out.println();

        // ── Q8: Print an Array of Student Objects ─────────────────────────────
        System.out.println("=== Q8: Print an Array of Student Objects ===");
        Student[] students = {
            new Student("Tom",  85),
            new Student("Mary", 90),
            new Student("John", 78)
        };
        for (Student s : students) {
            System.out.println(s);
        }
        System.out.println();

        // ── Q9: Update a Student's Score ──────────────────────────────────────
        System.out.println("=== Q9: Update a Student's Score ===");
        Student tom9 = new Student("Tom", 85);
        updateScore(tom9, 95);
        System.out.println(tom9);
        System.out.println();

        // ── Q10: Find the Minimum Score ───────────────────────────────────────
        System.out.println("=== Q10: Find the Minimum Score ===");
        int[] arr10 = {70, 85, 62, 90, 58};
        int min = arr10[0];
        for (int val : arr10) {
            if (val < min) min = val;
        }
        System.out.println(min);
    }
}