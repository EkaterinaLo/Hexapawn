package com.example.hexapawn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class Cell{
    float x1, y1, x2, y2;
    int status;
    boolean touched;
    Paint paint = new Paint();

    public Cell(float x1, float y1, float x2, float y2, int status, boolean touched) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.status = status;
        this.touched = touched;
    }

    public float getX1() {
        return x1;
    }

    public float getY1() {
        return y1;
    }

    public float getX2() {
        return x2;
    }

    public float getY2() {
        return y2;
    }

    public boolean isTouched() {
        return touched;
    }

    public void setTouched(boolean touched) {
        this.touched = touched;
    }

    public void setStatus(int status) {
        this.status = status;
    }



    public void drawPawn(Canvas canvas){
        if (status != 0) { //если в клетке есть пешка, рисуем её
            if (status == 1) {
                paint.setColor(Color.BLUE);
            }
            if (status == 2) {
                paint.setColor(Color.MAGENTA);
            }
            canvas.drawCircle(((x2 - x1) / 2) + x1, ((y2 - y1) / 2) + y1, (x2 - x1) / 3, paint);
        }
    }

    public void drawLight(Canvas canvas){ //подсветка пешки
        paint.setColor(Color.YELLOW);
        canvas.drawCircle(((x2 - x1) / 2) + x1, ((y2 - y1) / 2) + y1, (((x2 - x1) / 3) + 4), paint);
    }

    public boolean touchCell (float touchX, float touchY){
        if (touchX >= x1 && touchX <= x2 && touchY >= y1 && touchY <= y2){
            return true;
        } else {
            return false;
        }
    }
}

public class MyView extends View {
    float width;
    float height;
    float cellWidth;
    float cellHeight;
    Paint paint = new Paint();
    int[] colors; //массив цветов для доски
    int[][] steps; //массив расстановки
    ArrayList<Cell> cells = new ArrayList<>();
    View myView;
    Cell touchCell1; //ячейка которой коснулись 1
    Cell touchCell2; //ячейка которой коснулись 2
    boolean itIsNewGame = true;
    int lastStatus; //статус последнего игрока (1 или 2)
    Boolean gameWithComputer = true;
    final int PAUSE_LENGTH = 1; // длительность паузы в секундах
    boolean isOnPauseNow = false;
    HashMap <String, ArrayList<Arrow>> possibleStepsForSession = new HashMap<>(); //коллекция расстановок и возможных ходов
    Arrow lastArrow; //последний шаг сделанный компьютером
    ArrayList<Arrow> lastListOfArrows; //список шагов компьютера
    int [][] lastSteps; //последняя расстановка

    public MyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int whiteColor = Color.WHITE;
        int blackColor = Color.BLACK;
        colors = new int[9];
        for (int i = 0; i < colors.length; i++) { //цвета для доски
            if (i % 2 == 0){
                colors[i] = whiteColor;
            }
            else {
                colors[i] = blackColor;
            }
        }
        steps = new int[3][3];
        for (int i = 0; i < steps.length; i++) { //начальное положение пешек
            for (int j = 0; j < steps[i].length; j++) {
                steps[0][j] = 1;
                steps[2][j] = 2;
            }
        }


    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (itIsNewGame) {
            width = canvas.getWidth();
            height = canvas.getHeight();
            cellWidth = width / 3;
            cellHeight = height / 3;
            float x1 = 0;
            float y1 = 0;
            float x2 = cellWidth;
            float y2 = cellHeight;
            int k = 0; //счетчик для цветов доски
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    paint.setColor(colors[k]);
                    canvas.drawRect(x1, y1, x2, y2, paint); //отрисовка доски
                    cells.add(new Cell(x1, y1, x2, y2, steps[i][j], false));
                    x1 = x2;
                    x2 = x1 + cellWidth;
                    k++;
                }
                x1 = 0;
                x2 = cellWidth;
                y1 = y2;
                y2 = y1 + cellHeight;
            }
            for (Cell cell : cells) {
                cell.drawPawn(canvas); //отрисовка пешек
            }
            itIsNewGame = false;
        } else {
            int m = 0;
            for (Cell cell : cells) {
                paint.setColor(colors[m]);
                canvas.drawRect(cell.getX1(), cell.getY1(), cell.getX2(), cell.getY2(), paint); //отрисовка доски
                m++;
                if (cell.isTouched() && cell.status != 0){
                    cell.drawLight(canvas); //подсветка пешки, которую коснулись
                }
                cell.drawPawn(canvas); //отрисовка пешек
            }

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        int yCell = (int) (x / cellWidth); //размеры клетки
        int xCell = (int) (y / cellHeight);
        if (getCountOfTouched() > 1){
            retouch();
        }
        if (!gameWithComputer){ //игрок с игроком
            if (!itIsWin()){
                for (Cell cell: cells){
                    if (cell.touchCell(x, y)) {
                        if (getCountOfTouched() == 0) {
                            if (steps[xCell][yCell] == 2) { //если нажали на пешку
                                touchCell1 = cell;
                                touchCell1.setTouched(true);
                                return true;
                            }
                        }
                        if (getCountOfTouched() == 1) {
                            touchCell2 = cell;
                            touchCell2.setTouched(true);
                            int i1 = (int) (touchCell1.getY2()/cellHeight) - 1;
                            int j1 = (int) (touchCell1.getX2()/cellWidth) - 1;
                            int i2 = (int) (touchCell2.getY2()/cellHeight) - 1;
                            int j2 = (int) (touchCell2.getX2()/cellWidth) - 1;
                            if (touchCell2.status == 0) {
                                if ((touchCell1.status == 1) && (i2 >= touchCell1.status)){ //если нажали на пешку игрока 1
                                    stepUp(i1, j1, i2, j2); //ход вперёд
                                    return true;
                                } else {
                                    if ((touchCell1.status == 2) &&(i2 < touchCell1.status)){ //если нажали на пешку игрока 2
                                        stepUp(i1, j1, i2, j2);
                                        return true;
                                    } else {
                                        showWarning();
                                    }
                                }
                            } else {
                                cutDown(i1, j1, i2, j2); //снятие пешки
                                return true;
                            }
                        }
                    }
                }
            }
            else {
                win(); //сообщение о победе
            }
        } else { //игрок с компьютером
            if (!itIsWin()){
                for (Cell cell: cells){
                    if (cell.touchCell(x, y)) {
                        if (getCountOfTouched() == 0) {
                            if (steps[xCell][yCell] == 2) { //если нажали на розовую пешку
                                touchCell1 = cell;
                                touchCell1.setTouched(true);
                                invalidate();
                                return true;
                            }
                        }
                        if (getCountOfTouched() == 1) {
                            touchCell2 = cell;
                            touchCell2.setTouched(true);
                            int i1 = (int) (touchCell1.getY2()/cellHeight) - 1;
                            int j1 = (int) (touchCell1.getX2()/cellWidth) - 1;
                            int i2 = (int) (touchCell2.getY2()/cellHeight) - 1;
                            int j2 = (int) (touchCell2.getX2()/cellWidth) - 1;
                            if (touchCell2.status == 0) { //когда нажали на пустую клетку
                                if ((touchCell1.status == 2) &&(i2 < touchCell1.status)){
                                    stepUp(i1, j1, i2, j2); //шаг вперёд
                                } else {
                                    showWarning(); //сообщение об ошибке
                                }
                            }
                            if ((touchCell2.status == 1) && (i2 < i1)) {
                                cutDown(i1, j1, i2, j2); //снятие пешки
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public void win (){ //победа
        PauseTaskWin taskWin = new PauseTaskWin();
        taskWin.execute(PAUSE_LENGTH);
        isOnPauseNow = true;
    }

    public void stepUp (int i1, int j1, int i2, int j2){ //шаг вперёд
        if ((lastStatus != touchCell1.status) && (Math.abs(i2-i1) == 1) && (j1 == j2)) {
            steps[i2][j2] = steps[i1][j1];
            steps[i1][j1] = 0;
            touchCell1.setStatus(0);
            touchCell2.setStatus(steps[i2][j2]);
            lastStatus = touchCell2.status;
            invalidate();
            if (gameWithComputer) { //ход компьютера
                if (!itIsWin()) {
                    PauseTaskComputerStep task = new PauseTaskComputerStep();
                    task.execute(PAUSE_LENGTH);
                    isOnPauseNow = true;
                    if (itIsWin()) {
                        win();
                    }
                } else {
                    win();
                    lastListOfArrows.remove(lastArrow); //удаляем неверный шаг из списка шагов
                    String lastStepsStr = arrayToString(lastSteps);
                    possibleStepsForSession.remove(lastStepsStr);
                    possibleStepsForSession.put(lastStepsStr, lastListOfArrows); //обновляем список шагов в коллекции
                }
            }
        }
        else {
            showWarning();
        }
    }

    public void cutDown (int i1, int j1, int i2, int j2){ //снятие пешки
        if (lastStatus != touchCell1.status) {
            if (touchCell1.status != touchCell2.status) {
                if ((Math.abs(j2 - j1) == 1) && (Math.abs(i2 - i1) == 1)) {
                    lastStatus = touchCell1.status;
                    touchCell2.setStatus(touchCell1.status);
                    touchCell1.setStatus(0);
                    steps[i2][j2] = steps[i1][j1]; //обновляем массив расстановки
                    steps[i1][j1] = 0;
                    invalidate();
                    if (gameWithComputer) { //ход компьютера
                        if (!itIsWin()) {
                            PauseTaskComputerStep task = new PauseTaskComputerStep();
                            task.execute(PAUSE_LENGTH);
                            isOnPauseNow = true;
                            if (itIsWin()) {
                                win();
                            }
                        }
                        else {
                            win();
                            lastListOfArrows.remove(lastArrow); //удаляем неверный шаг из списка
                            String lastStepsStr = arrayToString(lastSteps);
                            possibleStepsForSession.remove(lastStepsStr);
                            possibleStepsForSession.put(lastStepsStr, lastListOfArrows); //обновляем коллекцию расстановок
                        }
                    }
                }
                else {
                    showWarning();
                }
            }
        }
        else {
            showWarning();
        }
    }

    public void showWarning(){ //сообщение об ошибке
        myView = findViewById(R.id.myView);
        Toast toast = Toast.makeText(myView.getContext(), "Ошибка!", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public int getCountOfTouched(){ //количество активных клеток
        int counter = 0;
        for (Cell cell : cells) {
            if (cell.isTouched()){
                counter++;
            }
        }
        return counter;
    }

    public void retouch (){ //сделать все клетки неактивными
        for (Cell cell : cells) {
            cell.setTouched(false);
        }
    }

    public boolean itIsWin(){ //проверка выигрыша
        int counterPawn1 = 0;
        int counterPawn2 = 0;
        for (int i = 0; i < steps.length; i++) {
            for (int j = 0; j < steps[i].length; j++) {
                if (steps[i][j] == 1){
                    counterPawn1++;
                }
                if (steps[i][j] == 2){
                    counterPawn2++;
                }
            }
        }
        if (counterPawn1 == 0){
            return true;
        }
        if (counterPawn2 == 0){
            return true;
        }

        int counterContrPawn1 = 0;
        int counterContrPawn2 = 0;
        for (int i = 0; i < steps.length; i++) {
            for (int j = 0; j < steps[i].length; j++) {
                if (steps[0][j] == 2){
                    counterContrPawn1++;
                }
                if (steps[2][j] == 1){
                    counterContrPawn2++;
                }
            }
        }

        if (counterContrPawn1 != 0){
            return true;
        }
        if (counterContrPawn2 != 0){
            return true;
        }
        int pawn1Counter = 0;
        int pawn2Counter = 0;
        int max1 = 0;
        int max2 = 0;
        /*Log.d("mytag", Arrays.toString(steps[0]));
        Log.d("mytag", Arrays.toString(steps[1]));
        Log.d("mytag", Arrays.toString(steps[2]));*/
        for (int i = 0; i < steps.length; i++) {
            for (int j = 0; j < steps[i].length; j++) {
                if (steps[i][j] == 1){
                    if ((i + 1 < 3) && (j - 1 < 0)) { //пешка в первом столбце
                        if (steps[i+1][j] != 0 && (steps[i+1][j+1]) != 2){
                            pawn1Counter++;
                        }
                    }
                    if ((i + 1 < 3) && (j - 1 > -1) && (j + 1 < 3)) {  //пешка по центру
                        if ((steps[i+1][j-1]) != 2 && steps[i+1][j] != 0   && (steps[i+1][j+1]) != 2){
                            pawn1Counter++;
                        }
                    }
                    if ((i + 1 < 3) && (j + 1 > 2)) { //пешка во втором столбце
                        if (steps[i+1][j-1] != 2 && steps[i+1][j] != 0){
                            pawn1Counter++;
                        }
                    }
                    max1++;
                }
                if (steps[i][j] == 2){
                    if ((i - 1 > -1) && (j - 1 < 0)) { //пешка в первом столбце
                        if (steps[i - 1][j] != 0 && (steps[i-1][j+1]) != 1){
                            pawn2Counter++;
                        }
                    }
                    if ((i - 1 > -1) && (j - 1 > -1) && (j + 1 < 3)) {  //пешка по центру
                        if ((steps[i-1][j-1]) != 1 && steps[i - 1][j] != 0   && (steps[i-1][j+1]) != 1){
                            pawn2Counter++;
                        }
                    }
                    if ((i - 1 > -1) && (j + 1 > 2)) { //пешка во втором столбце
                        if (steps[i-1][j-1] != 1 && steps[i - 1][j] != 0){
                            pawn2Counter++;
                        }
                    }
                    max2++;
                }
            }
        }
        if (((pawn1Counter == max1) && (pawn2Counter != max2) && lastStatus == 2) || ((pawn1Counter != max1) && (pawn2Counter == max2) && lastStatus == 1)) {
            return true;
        }
        if (pawn1Counter == max1 && pawn2Counter == max2){
            return true;
        }
        return false;
    }

    public void showWin(int lastStatus){ //сообщение об победе
        String gamer;
        if (lastStatus == 1){
            gamer = "cиних пешек";
        } else {
            gamer = "розовых пешек";
        }
        String text = "Победа " + gamer;
        myView = findViewById(R.id.myView);
        Toast toast = Toast.makeText(myView.getContext(), text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public void newGame (){ //новая игра
        printAllStepsStr();
        retouch();
        cells.clear();
        itIsNewGame = true;
        for (int i = 0; i < steps.length; i++) { //начальное положение пешек
            for (int j = 0; j < steps[i].length; j++) {
                steps[0][j] = 1;
                steps[1][j] = 0;
                steps[2][j] = 2;
            }
        }
        if (!gameWithComputer){
            lastStatus = 0;
        }
        else {
            lastStatus = 1;
        }
        invalidate();
    }

   class Arrow{ //класс стрелка для анализа возможных шагов для компьютера
        int iBeginCell;
        int jBeginCell;
        int iEndCell;
        int jEndCell;

       public Arrow(int iBeginCell, int jBeginCell, int iEndCell, int jEndCell) {
           this.iBeginCell = iBeginCell;
           this.jBeginCell = jBeginCell;
           this.iEndCell = iEndCell;
           this.jEndCell = jEndCell;
       }

       public int getiBeginCell() {
           return iBeginCell;
       }

       public int getjBeginCell() {
           return jBeginCell;
       }

       public int getiEndCell() {
           return iEndCell;
       }

       public int getjEndCell() {
           return jEndCell;
       }

       @Override
       public String toString() {
           return "Arrow{" + iBeginCell + " " + jBeginCell + " " + iEndCell + " " + jEndCell + '}';
       }
   }

   public ArrayList findPossibleSteps(){ //поиск возможных ходов
       ArrayList<Arrow> listOfArrows = new ArrayList<>();
       Arrow arrow;
       for (int i = 0; i < steps.length; i++) {
           for (int j = 0; j < steps[i].length; j++) {
               if (steps[i][j] == 1){
                   if ((i + 1 < 3) && (j - 1 < 0)) { //пешка в первом столбце
                       if (steps[i+1][j] == 0){
                           arrow = new Arrow(i, j, i+1, j);
                           listOfArrows.add(arrow);
                       }
                       if (steps[i+1][j+1] == 2){
                           arrow = new Arrow(i, j, i+1, j+1);
                           listOfArrows.add(arrow);
                       }
                   }
                   if ((i + 1 < 3) && (j - 1 > -1) && (j + 1 < 3)) {  //пешка по центру
                       if (steps[i+1][j-1] == 2){
                           arrow = new Arrow(i, j, i+1, j-1);
                           listOfArrows.add(arrow);
                       }
                       if (steps[i+1][j] == 0){
                           arrow = new Arrow(i, j, i+1, j);
                           listOfArrows.add(arrow);
                       }
                       if (steps[i+1][j+1] == 2){
                           arrow = new Arrow(i, j, i+1, j+1);
                           listOfArrows.add(arrow);
                       }

                   }
                   if ((i + 1 < 3) && (j + 1 > 2)) { //пешка во втором столбце
                       if (steps[i+1][j-1] == 2){
                           arrow = new Arrow(i, j, i+1, j-1);
                           listOfArrows.add(arrow);
                       }
                       if (steps[i+1][j] == 0){
                           arrow = new Arrow(i, j, i+1, j);
                           listOfArrows.add(arrow);
                       }
                   }
               }
           }
       }
       return listOfArrows;
   }

   public void doComputerStep (){ //шаг компьютера
       int randomIndexOfArrow = 0;
       ArrayList<Arrow> myArrows;
       String newStepsStr;
        if (!itIsWin()){
            int[][] newSteps;
            if (possibleStepsForSession.size() != 0){ //если играем не в первый раз
                newSteps = getSteps();
                newStepsStr = arrayToString(newSteps);
                myArrows = possibleStepsForSession.get(newStepsStr);
                if(myArrows != null){
                    randomIndexOfArrow = (int) (Math.random()*myArrows.size());
                } else{ //если такой расстановки ещё не было
                    myArrows = findPossibleSteps();
                    if (myArrows.size() != 0){
                        newSteps = getSteps();
                        newStepsStr = arrayToString(newSteps);
                        possibleStepsForSession.put(newStepsStr, myArrows); //запоминаем расстановку и возможные ходы
                        randomIndexOfArrow = (int) (Math.random()*myArrows.size());
                    }
                    else {
                        showWarning();
                    }
                }
                Arrow nextStep = myArrows.get(randomIndexOfArrow);
                lastArrow = nextStep; //запоминаем шаг
                lastListOfArrows = myArrows;
                lastSteps = getSteps();
                int i1 = nextStep.getiBeginCell();
                int j1 = nextStep.getjBeginCell();
                int i2 = nextStep.getiEndCell();
                int j2 = nextStep.getjEndCell();
                steps[i2][j2] = steps[i1][j1];
                steps[i1][j1] = 0;
                for (Cell cell : cells) {
                    if (cell.touchCell(j1*cellWidth+cellWidth/2, i1*cellHeight+cellHeight/2)) {
                        cell.setStatus(0);
                        cell.setTouched(false);
                    }
                    if (cell.touchCell(j2*cellWidth+cellWidth/2, i2*cellHeight+cellHeight/2)) {
                        cell.setStatus(steps[i2][j2]);
                        cell.setTouched(true);
                    }
                }
                lastStatus = 1;
                if (itIsWin()){

                    win();
                }
            } else { //если играем в первый раз
                myArrows = findPossibleSteps();
                if (myArrows.size() != 0){ //если возможно сделать шаг
                    newSteps = getSteps();
                    String key = arrayToString(newSteps);
                    possibleStepsForSession.put(key, myArrows); //запоминаем расстановку и возможные ходы
                    randomIndexOfArrow = (int) (Math.random()*myArrows.size());
                    Arrow nextStep = myArrows.get(randomIndexOfArrow);
                    lastArrow = nextStep; //запоминаем шаг
                    lastListOfArrows = myArrows;
                    lastSteps = getSteps();
                    int i1 = nextStep.getiBeginCell();
                    int j1 = nextStep.getjBeginCell();
                    int i2 = nextStep.getiEndCell();
                    int j2 = nextStep.getjEndCell();
                    steps[i2][j2] = steps[i1][j1];
                    steps[i1][j1] = 0;
                    for (Cell cell : cells) {
                        if (cell.touchCell(j1*cellWidth+cellWidth/2, i1*cellHeight+cellHeight/2)) {
                            cell.setStatus(0);
                            cell.setTouched(false);
                        }
                        if (cell.touchCell(j2*cellWidth+cellWidth/2, i2*cellHeight+cellHeight/2)) {
                            cell.setStatus(steps[i2][j2]);
                            cell.setTouched(true);
                        }
                    }
                    lastStatus = 1;
                    if (itIsWin()){
                        win();
                    }
                } else {
                    showWarning();
                }
            }
        }
    }

    public int[][] getSteps (){ //получить расстановку в виде массива
        int [][] currentSteps = new int [steps.length][steps[0].length];
        for (int i = 0; i < steps.length; i++) {
            for (int j = 0; j < steps[i].length; j++) {
               currentSteps[i][j] = steps[i][j];
           }
        }
        return currentSteps;
    }

    class PauseTaskComputerStep extends AsyncTask<Integer, Void, Void> { //пауза и ход компьютера
        protected Void doInBackground(Integer... integers) {
            try {
                Thread.sleep(integers[0] * 1000);
            } catch (InterruptedException e) {}
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            doComputerStep();
            isOnPauseNow = false;
            invalidate();
        }
    }

    class PauseTaskWin extends AsyncTask<Integer, Void, Void> { //пауза и сообщение о выигрыше
        protected Void doInBackground(Integer... integers) {
            try {
                Thread.sleep(integers[0] * 1000);
            } catch (InterruptedException e) {}
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            showWin(lastStatus);
            newGame(); //новая игра
            invalidate();
        }
    }

    public void printAllStepsStr (){ //вывод всех расстановок и шагов (для отладки)
        int i = 0;
        for (Map.Entry entry : possibleStepsForSession.entrySet()) {
            i++;
            ArrayList<Arrow> arrows = (ArrayList<Arrow>) entry.getValue();
            Log.d("mytag","№" + i);
            Log.d("mytag", "" + entry.getKey());
            for (Arrow ar:arrows) {
                Log.d("mytag",ar.toString());
            }
        }
    }

    public String arrayToString(int[][] arrayOfSteps){ //перевод массива в строку
        String strOfSteps = "";
        for (int i = 0; i < arrayOfSteps.length; i++) {
            for (int j = 0; j < arrayOfSteps[i].length; j++) {
                strOfSteps = strOfSteps.concat(Integer.toString(arrayOfSteps[i][j]));
            }
        }
        return strOfSteps;
    }
}
