package ch.idsia.agents;
import java.util.*;
import java.math.*;

import ch.idsia.agents.controllers.BasicMarioAIAgent;
import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.*;
import ch.idsia.agents.KeyOfMC;

public class MCAgent2 extends BasicMarioAIAgent implements Agent{
	static String name = "MCAgent";
	//前方2マスの縦何マスを取得するか
	public static final int width = 3;
	//取り得る行動の数
	public static final int numOfAction = 11;
	//J：ジャンプ　S：ファイア　R：右　L：左　D：下
	/*enum Action{
		J,
		S,
		R,
		L,
		D,
		JS,
		JR,
		JL,
		JD,
		JSR,
		JSL,
		NONE,
	}*/
	//毎フレームもっとも価値の高い行動をするが、確率epsilonで他の行動を等確率で選択
	public static float epsilon = 0.005f;
	//もっとも良い選択の再現に使用
	private static int frameCounter = 0;
	//毎エピソードで選択した行動を全フレーム分とっておく
	public static List<Integer> actions;
	//学習中にもっとも良かった行動群
	public static List<Integer> best;
	//学習中にもっとも良かったスコア
	public static float bestScore;
	//マリオの周りの状態とマリオが地面についているか
	private static int state = 0;
	//前1マスに崖があるか 0 : ない 1 : ある　 2: すげー壁
	private static int cliff = 0;
	//マリオがジャンプできるか 0 : できない 1 : できる
	private static int ableToJump = 0;
	//毎フレームで貪欲な選択をするかどうか
	public static boolean mode = false;
	//public static boolean momo = false;
	//各エピソードで、ある状態である行動を取ったかどうか KeyOfMCはint4つでstate,cliff,ableToJump,action
	//valueのIntegerはこのMCでは使わない
	public static HashMap<KeyOfMC,Integer> selected;
	//public static HashMap<KeyOfMC,Integer> Tselected;
	//行動価値関数　これを基に行動を決める
	public static float[][][][] qValue;
	//public static float[][][][] tValue;
	public static float finHeight;
	//各状態行動対におけるそれまで得た報酬の合計
	public static float[][][][] sumValue;
	//public static float[][][][] tSum;
	//ある状態である行動を取った回数
	public static int[][][][] num;
	//public static int[][][][] Tnum;
	
	public static int JM = 0;
	public static int ST = 0;
	public static int Cou = 0;
	public static float Change = 100;
	
	public static void setMode(boolean b){
		mode = b;
	}
	public static void ini(){
		frameCounter = 0;
		selected.clear();
		actions.clear();
	}
	
	//コンストラクタ
	public MCAgent2(){
		super(name);
		qValue = new float[(int)Math.pow(2.0,4 * width + 1)][3][2][numOfAction];
		//tValue = new float[(int)Math.pow(2.0,4 * width + 1)][3][2][numOfAction];
		sumValue = new float[(int)Math.pow(2.0,4 * width  + 1)][3][2][numOfAction];
		//tSum = new float[(int)Math.pow(2.0,4 * width  + 1)][3][2][numOfAction];
		num = new int[(int)Math.pow(2.0,4 * width + 1)][3][2][numOfAction];
		//Tnum = new int[(int)Math.pow(2.0,4 * width + 1)][3][2][numOfAction];
		selected = new HashMap<KeyOfMC,Integer>();
		//Tselected = new HashMap<KeyOfMC,Integer>();
		for(int i = 0; i < (int)Math.pow(2.0,4 * width + 1); ++i){
			for(int j = 0; j < 3; ++j){
				for(int k = 0; k < 2; ++k){
					for(int t = 0; t < numOfAction; ++t){
						qValue[i][j][k][t] = 0.0f;
						//tValue[i][j][k][t] = 0.0f;
						//一応全パターンは1回は試したいのである程度の値は持たせる
						sumValue[i][j][k][t] = 4096.0f;
						//tSum[i][j][k][t] = 50.0f;
						num[i][k][k][t] = 1;
						//Tnum[i][k][k][t] = 1;
					}
				}
			}
		}
		actions = new ArrayList<Integer>();
		best = new ArrayList<Integer>();
	}
	//行動価値関数を取得
	public static float[][][][] getQ(){
		return qValue;
	}
	//行動価値関数を取得
	//学習した後に再現で使う
	public static void setQ(float[][][][] q){
		qValue = q;
	}
	//障害物を検出し、stateの各bitに0,1で格納
	//ここでマリオが得る情報をほとんど決めている
	//ついでにマリオが地面にいるかも取得
	public void detectObstacle(){
		state = 0;
		for(int j = 0; j < width; ++j){
			if(getEnemiesCellValue(marioEgoRow + j - 1,marioEgoCol + 1) != Sprite.KIND_NONE)
				state += (int)Math.pow(2,j);
		}
		for(int j = 0; j < width; ++j){
			if(getReceptiveFieldCellValue(marioEgoRow + j - 1,marioEgoCol + 1) != 0)
				state += (int)Math.pow(2,width + j);
		}
		for(int j = 0; j < width; ++j){
			if(getEnemiesCellValue(marioEgoRow + j - 1,marioEgoCol + 2) != Sprite.KIND_NONE)
				state += (int)Math.pow(2, 2 * width + j);
		}
		for(int j = 0; j < width; ++j){
			if(getReceptiveFieldCellValue(marioEgoRow + j - 1,marioEgoCol + 2) != 0)
				state += (int)Math.pow(2,3 * width + j);
		}
		if(isMarioOnGround)
			state += (int)Math.pow(2, 4 * width);
		
	}
	//boolをintへ
	public int boolToInt(boolean b){
		return (b) ? 1 : 0;
	}
	//崖検出
	public void detectCliff(){
		for(int i = 0; i < 10; ++i){
			if(getReceptiveFieldCellValue(marioEgoRow + i,marioEgoCol + 1) == 0 && i == 0){
				break;
			}else{
				if( getReceptiveFieldCellValue(marioEgoRow + i,marioEgoCol + 1) == 0 && i < 5 ){
					cliff = 1;
					break;
				}else if( i >= 5 ){
					cliff = 2;
					break;
				}
			}
		}
		
		/*boolean b = true;
		for(int i = 0; i < 10; ++i){
			if(getReceptiveFieldCellValue(marioEgoRow + i,marioEgoCol + 1) != 0){
				b = false;
				break;
			}
		}
		cliff = (b) ? 1 : 0;*/
	}
	
	//行動価値関数を基に行動選択
	public int chooseAction(){
		float r = (float)(Math.random());
		int idx = 0;
		
		if(r < epsilon ){
			float sum = 0;
			float d = epsilon / (float)numOfAction;
			sum += d;
			while(sum < r){
				sum += d;
				idx++;
			}
			
		}else if( Cou > 100 ){
			idx = 9;
			Cou = 0;
		}else{
			float max = -Float.MAX_VALUE;
			for(int i = 0; i < numOfAction; ++i){
				float q = qValue[state][cliff][ableToJump][i];
				if(q > max){
					max = q;
					idx = i;
				}
			}
			
			
			if( Change == distancePassedPhys ){
				Cou++;
			}
			Change = distancePassedPhys;
			
		}
		
		
		return idx;
	}
	
	
	//貪欲に行動を選択
	public int chooseActionG(){
		int idx = 0;
		float max = -Float.MAX_VALUE;
			for(int j = 0; j < numOfAction; ++j){
				float q = qValue[state][cliff][ableToJump][j];
				if(q > max){
					max = q;
					idx = j;
				}
			}
		return idx;
	}
	//行動選択前にactionを一旦全部falseにする
	public void clearAction(){
		for(int i = 0; i < Environment.numberOfKeys; ++i){
			action[i] = false;
		}
	}
	//int(0-10)をacitonにする
	public void intToAction(int n){
		if(n == 0 || (n > 4 && n < 11))
			action[Mario.KEY_JUMP] = true;
		if(n == 1 || n == 5 || n == 9 || n == 10)
			action[Mario.KEY_SPEED] = true;
		if(n == 2 || n == 6 || n == 9)
			action[Mario.KEY_RIGHT] = true;
		if(n == 3 || n == 7 || n == 10)
			action[Mario.KEY_LEFT] = true;
 		if(n == 4 || n == 8)
			action[Mario.KEY_DOWN] = true;
	}
	
	public boolean isObstacle(int r, int c){
		return getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.BRICK
				|| getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH
				|| getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.FLOWER_POT_OR_CANNON
				|| getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.LADDER;
	}
	
	
	public boolean[] getAction(){
		
		
		if(!mode){
			if( distancePassedPhys > 1590 && distancePassedPhys < 2100 ){
				int y = marioEgoCol;
				int x = marioEgoRow;
				
				if( isMarioOnGround ){
						//System.out.println("地面");
						action[Mario.KEY_RIGHT ] = true;
						action[Mario.KEY_LEFT] = false;
						action[Mario.KEY_JUMP] = false;
						action[Mario.KEY_SPEED] = false;
						JM = 0;
				}
				
				if( getEnemiesCellValue(x ,y+1) != 0 || getEnemiesCellValue(x ,y+2) != 0 && isMarioAbleToJump ){
					action[Mario.KEY_JUMP] = true;
					JM = 1;
				}
				
				if(!isMarioAbleToJump  &&  getEnemiesCellValue(x ,y) != 0 ){
						//action[Mario.KEY_LEFT] = true;
						action[Mario.KEY_RIGHT] = false;
				}
				
				
				//ジャンプ系
				if(  isObstacle( x, y+1) && !isObstacle( x-1,y+1) && marioFloatPos[1] > 15 ){
					if( JM == 0 ){
						action[Mario.KEY_JUMP] = isMarioAbleToJump;//障害物避ける
					}
				}else if( isObstacle( x,y+1 ) && isObstacle( x-1,y+1 ) ){
					action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround ;
					JM = 1;
				}else if( getReceptiveFieldCellValue( x+1 , y+1) == 0  && getReceptiveFieldCellValue( x+1, y+4) != 0 
						&& isMarioOnGround && isMarioAbleToJump){ 
					action[Mario.KEY_JUMP] = true;
					JM = 1;
				}
				
			}else{
				detectObstacle();
				detectCliff();
				ableToJump = boolToInt(isMarioAbleToJump);
				clearAction();
				int currAction = 0;
				
				currAction = chooseAction();
				//System.out.println( currAction );
				actions.add(currAction);
				intToAction(currAction);
				
				if(!selected.containsKey(new KeyOfMC(state,cliff,ableToJump,currAction)))
					selected.put(new KeyOfMC(state,cliff,ableToJump,currAction),1);	
				else
					selected.put(new KeyOfMC(state,cliff,ableToJump,currAction), selected.get(new KeyOfMC(state,cliff,ableToJump,currAction)) + 1);
				frameCounter++;
				}
		}else{
			if( distancePassedPhys > 1590 && distancePassedPhys < 2100 ){
				int y = marioEgoCol;
				int x = marioEgoRow;
				
				if( isMarioOnGround ){
						//System.out.println("地面");
						action[Mario.KEY_RIGHT ] = true;
						action[Mario.KEY_LEFT] = false;
						action[Mario.KEY_JUMP] = false;
						action[Mario.KEY_SPEED] = false;
						JM = 0;
				}
				
				if( getEnemiesCellValue(x ,y+1) != 0 || getEnemiesCellValue(x ,y+2) != 0 && isMarioAbleToJump ){
					action[Mario.KEY_JUMP] = true;
					JM = 1;
				}
				
				if(!isMarioAbleToJump  &&  getEnemiesCellValue(x ,y) != 0 ){
						//action[Mario.KEY_LEFT] = true;
						action[Mario.KEY_RIGHT] = false;
				}
				
				
				//ジャンプ系
				if(  isObstacle( x, y+1) && !isObstacle( x-1,y+1) && marioFloatPos[1] > 15 ){
					if( JM == 0 ){
						action[Mario.KEY_JUMP] = isMarioAbleToJump;//障害物避ける
					}
				}else if( isObstacle( x,y+1 ) && isObstacle( x-1,y+1 ) ){
					action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround ;
					JM = 1;
				}else if( getReceptiveFieldCellValue( x+1 , y+1) == 0  && getReceptiveFieldCellValue( x+1, y+4) != 0 
						&& isMarioOnGround && isMarioAbleToJump){ 
					action[Mario.KEY_JUMP] = true;
					JM = 1;
				}
				
			}else{
			
				detectObstacle();
				detectCliff();
				ableToJump = boolToInt(isMarioAbleToJump);
				clearAction();
				int currAction = 0;
				System.out.println("dis" + distancePassedPhys);
				if(frameCounter < best.size())
					currAction = best.get(frameCounter);
				intToAction(currAction);
				frameCounter++;
			}
		}
		
		return action;
	}
}