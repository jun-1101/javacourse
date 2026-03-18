import java.util.Scanner;

public class MatrixMultiplication {

    /**
     * 矩陣乘法方法
     * 計算兩個矩陣 A 和 B 的乘積 C = A * B。
     * 要求：矩陣 A 的列數必須等於矩陣 B 的行數。
     * 
     * @param A 第一個矩陣 (m x n)
     * @param B 第二個矩陣 (n x p)
     * @return 乘積矩陣 C (m x p)
     * @throws IllegalArgumentException 如果矩陣維度不匹配，則拋出異常
     */
    public static double[][] matrixMultiply(double[][] A, double[][] B) {
        // 獲取矩陣 A 的維度
        int rowsA = A.length;
        int colsA = A[0].length;
        // 獲取矩陣 B 的維度
        int rowsB = B.length;
        int colsB = B[0].length;

        // 檢查矩陣維度是否匹配乘法要求
        if (colsA != rowsB) {
            throw new IllegalArgumentException(
                "矩陣維度不匹配，無法進行乘法。矩陣 A 的列數 (" + colsA + ") 必須等於矩陣 B 的行數 (" + rowsB + ")。" );
        }

        // 初始化結果矩陣 C，其維度為 (rowsA x colsB)
        double[][] C = new double[rowsA][colsB];

        // 執行矩陣乘法的三重巢狀迴圈
        // 外層迴圈遍歷結果矩陣 C 的行
        for (int i = 0; i < rowsA; i++) {
            // 中間層迴圈遍歷結果矩陣 C 的列
            for (int j = 0; j < colsB; j++) {
                // 最內層迴圈執行點積求和
                for (int k = 0; k < colsA; k++) { // k 可以遍歷 colsA 或 rowsB，因為它們相等
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }

    /**
     * 輔助方法：列印矩陣
     * 以格式化的方式將二維 double 陣列列印到控制台。
     * 
     * @param matrix 要列印的矩陣
     */
    public static void printMatrix(double[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            System.out.println("空矩陣或無效矩陣。");
            return;
        }
        for (double[] row : matrix) {
            for (double val : row) {
                System.out.printf("%.4f\t", val); // 格式化輸出，保留四位小數並使用 tab 分隔
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== 矩陣乘法測試 ===\n");

        // 範例 1: 2x3 矩陣乘以 3x2 矩陣，結果為 2x2 矩陣
        double[][] A1 = {{1, 2, 3}, {4, 5, 6}};
        double[][] B1 = {{7, 8}, {9, 10}, {11, 12}};

        System.out.println("矩陣 A1:");
        printMatrix(A1);
        System.out.println("\n矩陣 B1:");
        printMatrix(B1);

        try {
            double[][] C1 = matrixMultiply(A1, B1);
            System.out.println("\n矩陣 A1 * B1 的結果 C1:");
            printMatrix(C1);
        } catch (IllegalArgumentException e) {
            System.err.println("錯誤: " + e.getMessage());
        }

        System.out.println("\n------------------------------------\n");

        // 範例 2: 3x3 矩陣乘以 3x1 矩陣 (向量)，結果為 3x1 矩陣
        double[][] A2 = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}; // 單位矩陣
        double[][] B2 = {{5}, {10}, {15}};

        System.out.println("矩陣 A2 (單位矩陣):");
        printMatrix(A2);
        System.out.println("\n矩陣 B2 (向量):");
        printMatrix(B2);

        try {
            double[][] C2 = matrixMultiply(A2, B2);
            System.out.println("\n矩陣 A2 * B2 的結果 C2:");
            printMatrix(C2);
        } catch (IllegalArgumentException e) {
            System.err.println("錯誤: " + e.getMessage());
        }

        System.out.println("\n------------------------------------\n");

        // 範例 3: 維度不匹配的矩陣乘法 (預期會拋出異常)
        double[][] A3 = {{1, 2}, {3, 4}};
        double[][] B3 = {{5, 6, 7}, {8, 9, 10}, {11, 12, 13}};

        System.out.println("矩陣 A3:");
        printMatrix(A3);
        System.out.println("\n矩陣 B3:");
        printMatrix(B3);

        try {
            System.out.println("\n嘗試計算矩陣 A3 * B3 (預期失敗):");
            double[][] C3 = matrixMultiply(A3, B3);
            printMatrix(C3);
        } catch (IllegalArgumentException e) {
            System.err.println("成功捕獲錯誤: " + e.getMessage());
        }

        scanner.close();
    }
}
