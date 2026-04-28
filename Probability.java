// =====================================================
// Assignment 1: Probability
// 每頁講義對應一個 class，實際帶入數字計算並輸出結果
// =====================================================

// Page 2: 機率的定義
class ProbabilityDefinition {
    String title = "1. 機率的定義";
    int favorable; // 事件A發生的個數
    int total;     // 全部可能情況

    ProbabilityDefinition(int favorable, int total) {
        this.favorable = favorable;
        this.total = total;
    }

    double calculate() {
        return (double) favorable / total;
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• 機率表示某件事情發生的可能性");
        System.out.println("• 公式：P(A) = 事件A發生的個數 / 全部可能情況");
        System.out.println("• 範例：骰子擲出3點");
        System.out.println("  → 有利結果數 = " + favorable + "，全部結果數 = " + total);
        System.out.printf("  → P(擲出3點) = %d / %d = %.4f%n", favorable, total, calculate());
        System.out.println();
    }
}

// Page 3: 樣本空間
class SampleSpace {
    String title = "2. 樣本空間（Sample Space）";
    String[] space; // 樣本空間 S

    SampleSpace(String[] space) {
        this.space = space;
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• S：所有可能結果的集合");
        System.out.print("• 樣本空間 S = { ");
        for (int i = 0; i < space.length; i++) {
            System.out.print(space[i]);
            if (i < space.length - 1) System.out.print(", ");
        }
        System.out.println(" }");
        System.out.println("• |S| = " + space.length + " 個元素");
        System.out.println();
    }
}

// Page 4: 事件
class Event {
    String title = "3. 事件（Event）";
    String[] sampleSpace;
    String[] eventA; // 我們關心的事件

    Event(String[] sampleSpace, String[] eventA) {
        this.sampleSpace = sampleSpace;
        this.eventA = eventA;
    }

    double probability() {
        return (double) eventA.length / sampleSpace.length;
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• A：我們關心的事件");
        System.out.print("• 事件 A = { ");
        for (int i = 0; i < eventA.length; i++) {
            System.out.print(eventA[i]);
            if (i < eventA.length - 1) System.out.print(", ");
        }
        System.out.println(" }（偶數點）");
        System.out.println("• |A| = " + eventA.length + "，|S| = " + sampleSpace.length);
        System.out.printf("• P(A) = %d / %d = %.4f%n", eventA.length, sampleSpace.length, probability());
        System.out.println();
    }
}

// Page 5: 基本公式
class BasicFormula {
    String title = "4. 基本公式";
    int nA; // n(A)
    int nS; // n(S)

    BasicFormula(int nA, int nS) {
        this.nA = nA;
        this.nS = nS;
    }

    double calculate() {
        return (double) nA / nS;
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• 公式：P(A) = n(A) / n(S)");
        System.out.println("• 口訣：我要的 / 全部的");
        System.out.println("• 範例：一副撲克牌(52張)，抽到紅心的機率");
        System.out.println("  → n(A) = " + nA + "（紅心有13張）");
        System.out.println("  → n(S) = " + nS + "（共52張）");
        System.out.printf("  → P(抽到紅心) = %d / %d = %.4f%n", nA, nS, calculate());
        System.out.println();
    }
}

// Page 6: 補事件
class ComplementEvent {
    String title = "5. 補事件";
    double pA;

    ComplementEvent(double pA) {
        this.pA = pA;
    }

    double calculate() {
        return 1 - pA;
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• 公式：P(A^c) = 1 - P(A)");
        System.out.println("• 範例：不抽到紅心的機率");
        System.out.printf("  → P(紅心) = %.4f%n", pA);
        System.out.printf("  → P(非紅心) = 1 - %.4f = %.4f%n", pA, calculate());
        System.out.println();
    }
}

// Page 7: 聯集（OR）
class UnionEvent {
    String title = "6. 聯集（OR）";
    double pA;
    double pB;
    double pAandB;

    UnionEvent(double pA, double pB, double pAandB) {
        this.pA = pA;
        this.pB = pB;
        this.pAandB = pAandB;
    }

    double calculate() {
        return pA + pB - pAandB;
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• 公式：P(A∪B) = P(A) + P(B) - P(A∩B)");
        System.out.println("• 範例：抽到紅心或人頭牌（J、Q、K、A）的機率");
        System.out.printf("  → P(A) = P(紅心) = %.4f%n", pA);
        System.out.printf("  → P(B) = P(人頭) = %.4f%n", pB);
        System.out.printf("  → P(A∩B) = P(紅心人頭) = %.4f%n", pAandB);
        System.out.printf("  → P(A∪B) = %.4f + %.4f - %.4f = %.4f%n",
                          pA, pB, pAandB, calculate());
        System.out.println();
    }
}

// Page 8: 交集（AND）
class IntersectionEvent {
    String title = "7. 交集（AND）";
    double pA;
    double pBgivenA;

    IntersectionEvent(double pA, double pBgivenA) {
        this.pA = pA;
        this.pBgivenA = pBgivenA;
    }

    double calculate() {
        return pA * pBgivenA;
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• 公式：P(A∩B) = P(A) * P(B|A)");
        System.out.println("• 範例：連續抽兩張牌，都是紅心（不放回）");
        System.out.printf("  → P(A) = P(第1張是紅心) = %.4f%n", pA);
        System.out.printf("  → P(B|A) = P(第2張是紅心|第1張已是紅心) = %.4f%n", pBgivenA);
        System.out.printf("  → P(A∩B) = %.4f × %.4f = %.4f%n",
                          pA, pBgivenA, calculate());
        System.out.println();
    }
}

// Page 9: 條件機率
class ConditionalProbability {
    String title = "8. 條件機率";
    double pAandB;
    double pB;

    ConditionalProbability(double pAandB, double pB) {
        this.pAandB = pAandB;
        this.pB = pB;
    }

    double calculate() {
        return pAandB / pB;
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• 公式：P(A|B) = P(A∩B) / P(B)");
        System.out.println("• 意思：已知B發生，A的機率");
        System.out.println("• 範例：已知抽到人頭牌，是紅心的機率");
        System.out.printf("  → P(A∩B) = P(紅心人頭) = %.4f%n", pAandB);
        System.out.printf("  → P(B) = P(人頭) = %.4f%n", pB);
        System.out.printf("  → P(A|B) = %.4f / %.4f = %.4f%n",
                          pAandB, pB, calculate());
        System.out.println();
    }
}

// Page 10: 獨立事件
class IndependentEvents {
    String title = "9. 獨立事件";
    double pA;
    double pB;

    IndependentEvents(double pA, double pB) {
        this.pA = pA;
        this.pB = pB;
    }

    double calculateIntersection() {
        return pA * pB;
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• 若A、B獨立：P(A∩B) = P(A) * P(B)，且 P(A|B) = P(A)");
        System.out.println("• 範例：投硬幣與擲骰子，各自獨立");
        System.out.printf("  → P(A) = P(正面) = %.4f%n", pA);
        System.out.printf("  → P(B) = P(擲出6點) = %.4f%n", pB);
        System.out.printf("  → P(A∩B) = %.4f × %.4f = %.4f%n",
                          pA, pB, calculateIntersection());
        System.out.printf("  → P(A|B) = P(A) = %.4f（因為獨立，B不影響A）%n", pA);
        System.out.println();
    }
}

// Page 11: 貝氏定理
class BayesTheorem {
    String title = "10. 貝氏定理";
    double pA;
    double pBgivenA;
    double pBgivenNotA;

    BayesTheorem(double pA, double pBgivenA, double pBgivenNotA) {
        this.pA = pA;
        this.pBgivenA = pBgivenA;
        this.pBgivenNotA = pBgivenNotA;
    }

    double pB() {
        return pBgivenA * pA + pBgivenNotA * (1 - pA);
    }

    double calculate() {
        return (pBgivenA * pA) / pB();
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• 公式：P(A|B) = P(B|A) * P(A) / P(B)");
        System.out.println("• 用途：已知結果，反推原因");
        System.out.println("• 範例：疾病篩檢");
        System.out.println("  → 某疾病盛行率 P(患病) = " + pA);
        System.out.println("  → 篩檢陽性率（真陽）P(陽性|患病) = " + pBgivenA);
        System.out.println("  → 篩檢偽陽率 P(陽性|未患病) = " + pBgivenNotA);
        System.out.printf("  → P(B) = %.2f×%.2f + %.2f×%.2f = %.4f%n",
                          pBgivenA, pA, pBgivenNotA, (1 - pA), pB());
        System.out.printf("  → P(患病|陽性) = %.4f%n", calculate());
        System.out.println();
    }
}

// Page 12: 全機率公式
class TotalProbability {
    String title = "11. 全機率公式";
    double[] pBi;
    double[] pAgivenBi;
    String[] labels;

    TotalProbability(double[] pBi, double[] pAgivenBi, String[] labels) {
        this.pBi = pBi;
        this.pAgivenBi = pAgivenBi;
        this.labels = labels;
    }

    double calculate() {
        double total = 0;
        for (int i = 0; i < pBi.length; i++) {
            total += pAgivenBi[i] * pBi[i];
        }
        return total;
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• 公式：P(A) = Σ P(A|Bi) * P(Bi)");
        System.out.println("• 範例：工廠三條產線生產燈泡，求隨機抽到瑕疵品的機率");
        for (int i = 0; i < pBi.length; i++) {
            System.out.printf("  → P(%s) = %.2f，P(瑕疵|%s) = %.2f%n",
                              labels[i], pBi[i], labels[i], pAgivenBi[i]);
        }
        System.out.printf("  → P(瑕疵) = Σ = %.4f%n", calculate());
        System.out.println();
    }
}

// Page 13: 學校例子
class SchoolExample {
    String title = "12. 學校例子";
    int jianzhong;
    int beiyi;
    int total;

    SchoolExample(int jianzhong, int beiyi, int total) {
        this.jianzhong = jianzhong;
        this.beiyi = beiyi;
        this.total = total;
    }

    double pJianzhong() { return (double) jianzhong / total; }
    double pBeiyi()     { return (double) beiyi / total; }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• 交大中：建中 J 人，北一女 B 人，總人數 N");
        System.out.println("  → 建中人數 J = " + jianzhong);
        System.out.println("  → 北一女人數 B = " + beiyi);
        System.out.println("  → 總人數 N = " + total);
        System.out.printf("  → P(建中) = %d / %d = %.4f%n", jianzhong, total, pJianzhong());
        System.out.printf("  → P(北一女) = %d / %d = %.4f%n", beiyi, total, pBeiyi());
        System.out.println();
    }
}

// Page 14: 建中或北一女
class JianzhongOrBeiyi {
    String title = "13. 建中或北一女";
    int jianzhong;
    int beiyi;
    int total;

    JianzhongOrBeiyi(int jianzhong, int beiyi, int total) {
        this.jianzhong = jianzhong;
        this.beiyi = beiyi;
        this.total = total;
    }

    double calculate() {
        return (double)(jianzhong + beiyi) / total;
    }

    void display() {
        System.out.println("===== " + title + " =====");
        System.out.println("• 公式：P(建中∪北一女) = (J + B) / N（因為兩者互斥）");
        System.out.printf("  → (%d + %d) / %d = %d / %d = %.4f%n",
                          jianzhong, beiyi, total,
                          jianzhong + beiyi, total, calculate());
        System.out.printf("  → 隨機抽一人，是建中或北一女的機率 = %.2f%%%n",
                          calculate() * 100);
        System.out.println();
    }
}

// =====================================================
// Main Class
// =====================================================
public class Probability {
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║      Assignment 1 – Probability          ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        // Page 2: 骰子擲出3點，1個有利 / 共6面
        ProbabilityDefinition page2 = new ProbabilityDefinition(1, 6);

        // Page 3: 骰子樣本空間 {1,2,3,4,5,6}
        SampleSpace page3 = new SampleSpace(
            new String[]{"1","2","3","4","5","6"}
        );

        // Page 4: 事件A = 骰子偶數面 {2,4,6}
        Event page4 = new Event(
            new String[]{"1","2","3","4","5","6"},
            new String[]{"2","4","6"}
        );

        // Page 5: 撲克牌抽到紅心，13/52
        BasicFormula page5 = new BasicFormula(13, 52);

        // Page 6: 補事件，P(紅心) = 13/52
        ComplementEvent page6 = new ComplementEvent(13.0 / 52);

        // Page 7: 聯集，紅心 OR 人頭（J/Q/K/A）
        // P(紅心)=13/52, P(人頭)=16/52, P(紅心∩人頭)=4/52
        UnionEvent page7 = new UnionEvent(13.0/52, 16.0/52, 4.0/52);

        // Page 8: 交集，連抽兩張都是紅心（不放回）
        IntersectionEvent page8 = new IntersectionEvent(13.0/52, 12.0/51);

        // Page 9: 條件機率，已知人頭 → 是紅心
        ConditionalProbability page9 = new ConditionalProbability(4.0/52, 16.0/52);

        // Page 10: 獨立事件，硬幣正面 vs 骰子6點
        IndependentEvents page10 = new IndependentEvents(0.5, 1.0/6);

        // Page 11: 貝氏定理，疾病篩檢
        // P(患病)=0.01, P(陽性|患病)=0.95, P(陽性|未患病)=0.05
        BayesTheorem page11 = new BayesTheorem(0.01, 0.95, 0.05);

        // Page 12: 全機率，三條產線
        TotalProbability page12 = new TotalProbability(
            new double[]{0.50, 0.30, 0.20},
            new double[]{0.02, 0.03, 0.05},
            new String[]{"產線B1","產線B2","產線B3"}
        );

        // Page 13 & 14: 學校例子，建中50人，北一女30人，總200人
        SchoolExample page13 = new SchoolExample(50, 30, 200);
        JianzhongOrBeiyi page14 = new JianzhongOrBeiyi(50, 30, 200);

        // 依序呼叫 display() 顯示計算結果
        page2.display();
        page3.display();
        page4.display();
        page5.display();
        page6.display();
        page7.display();
        page8.display();
        page9.display();
        page10.display();
        page11.display();
        page12.display();
        page13.display();
        page14.display();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║            END OF OUTPUT                 ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }
}