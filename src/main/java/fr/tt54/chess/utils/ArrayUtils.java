package fr.tt54.chess.utils;

public class ArrayUtils {

    public static void print2DArray(int[][] array){
        for(int i = array.length - 1; i >= 0; i--){
            for(int j = 0; j < array[i].length; j++){
                if(j > 0){
                    System.out.print(',');
                }
                System.out.print(array[i][j]);
            }
            System.out.println();
        }
    }

}
