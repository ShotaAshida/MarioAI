package ch.idsia.agents;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.benchmark.tasks.LearningTask;
import ch.idsia.tools.EvaluationInfo;
import ch.idsia.tools.MarioAIOptions;
import ch.idsia.utils.wox.serial.Easy;
import ch.idsia.agents.MCAgent2;
import ch.idsia.agents.KeyOfMC;

public class LearningWithMC2 implements LearningAgent{
	private Agent agent;
	private String name = "LearningWithMC";
	//目標値(4096.0はステージの右端)
	private float goal = 4096.0f;
	private String args;
	//試行回数
	private int numOfTrial = 50000;
	//コンストラクタ
	public LearningWithMC2(String args){
		this.args = args;
		agent = new MCAgent2();
	}
	//学習部分
	//1000回学習してその中でもっとも良かったものをリプレイ
	public void learn(){
		for(int i = 0; i < numOfTrial; ++i){
			//目標値までマリオが到達したらshowして終了
			
			if(run() >= 4096.0f){
				for( int j = 0 ; j < MCAgent2.best.size(); j++ ){
					System.out.print(MCAgent2.best.get(j)+ " ");
				}
				show();
			}
			if( i % 1000 == 999 )
				show();
		}

		try{
			//学習した行動価値関数を書き込み
			File f = new File("MonteCarlo.txt");
			f.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			for(int i = 0; i < Math.pow(2.0, MCAgent2.width * 4 + 1); ++i){
				for(int j = 0; j < 2; ++j){
					for(int k = 0;k < 2; ++k){
						for(int t = 0; t < MCAgent2.numOfAction; ++t){
							bw.write(String.valueOf(MCAgent2.qValue[i][j][k][t]));
							bw.newLine();
						}
					}
				}
			}
		}
		catch(IOException e){
		    System.out.println(e);
		}
	}
	
	
	//リプレイ
	public void show(){
		MCAgent2.ini();
		MarioAIOptions marioAIOptions = new MarioAIOptions();
		BasicTask basicTask = new BasicTask(marioAIOptions);

		/* ステージ生成 */
		marioAIOptions.setArgs(this.args);
		MCAgent2.setMode(true);


	    /* プレイ画面出力するか否か */
	    marioAIOptions.setVisualization(true);
		/* MCAgentをセット */
		marioAIOptions.setAgent(agent);
		basicTask.setOptionsAndReset(marioAIOptions);

		if ( !basicTask.runSingleEpisode(1) ){
			System.out.println("MarioAI: out of computational time"
			+ " per action! Agent disqualified!");
		}

		/* 評価値(距離)をセット */
		EvaluationInfo evaluationInfo = basicTask.getEvaluationInfo();
		//報酬取得
		float reward = evaluationInfo.distancePassedPhys;
		System.out.println("報酬は" + reward);
	}
	
	
	//学習
	//画面に表示はしない
	public float run(){
		MCAgent2.ini();
		/* MCAgentをプレイさせる */
		MarioAIOptions marioAIOptions = new MarioAIOptions();
		BasicTask basicTask = new BasicTask(marioAIOptions);

		/* ステージ生成 */
		marioAIOptions.setArgs(this.args);
		MCAgent2.setMode(false);


	    /* プレイ画面出力するか否か */
	    marioAIOptions.setVisualization(false);
		/* MCAgentをセット */
		marioAIOptions.setAgent(agent);
		basicTask.setOptionsAndReset(marioAIOptions);

		if ( !basicTask.runSingleEpisode(1) ){
			System.out.println("MarioAI: out of computational time"
			+ " per action! Agent disqualified!");
		}

		/* 評価値(距離)をセット */
		EvaluationInfo evaluationInfo = basicTask.getEvaluationInfo();
		//報酬取得
		float reward = evaluationInfo.distancePassedPhys;
		
		
		
		/*float dis = 0;
		if( reward > 1650 ){
			float tReward = reward - 1650;
			//dis =(float)Math.sqrt( tReward*tReward +(200- MCAgent.finHeight)*(200-MCAgent.finHeight)  );
			dis = (float)Math.pow(200 - MCAgent.finHeight,2);
			//System.out.println( "tReward"+tReward);
			System.out.println("finheight"+MCAgent.finHeight);
			System.out.println("dis" + dis);
		}*/
		
		//float time = evaluationInfo.timeSpent;
		//System.out.println("jikann"+ time);
		//reward -= (evaluationInfo.marioStatus == 0) ? 1000 : 0;
		
		System.out.println(reward);
		
		/*float kati = MCAgent.velo;
		System.out.println("はやさ"+ kati);*/
		
		//ベストスコアが出たら更新
		if(reward > MCAgent2.bestScore){
			MCAgent2.bestScore = reward;
			MCAgent2.best = new ArrayList<Integer>(MCAgent2.actions);
			for(int i = 0 ; i < MCAgent2.best.size(); i++){
				if( i < MCAgent2.best.size()-1 ){
					System.out.print( MCAgent2.best.get(i) + " " );
				}else{
					System.out.println( MCAgent2.best.get(i) );
				}
			}
		}
		
		
		
		//価値関数を更新
		//System.out.println("tValue");
			
		
		/*if( reward  > 1650 && reward < 1700 ){
			Iterator<KeyOfMC> Titr = MCAgent.Tselected.keySet().iterator();
			while(Titr.hasNext()){
				KeyOfMC key = (KeyOfMC)Titr.next();
				MCAgent.tSum[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]+= dis;
				MCAgent.Tnum[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]++;
				MCAgent.tValue[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()] =
						MCAgent.tSum[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]
								/ (float)MCAgent.Tnum[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()];
			}
		}else{
			Iterator<KeyOfMC> itr = MCAgent.selected.keySet().iterator();
			while(itr.hasNext()){
				KeyOfMC key = (KeyOfMC)itr.next();
				MCAgent.sumValue[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]+= reward;
				MCAgent.num[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]++;
				MCAgent.qValue[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()] =
						MCAgent.sumValue[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]
								/ (float)MCAgent.num[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()];
			}
		}*/
		
		Iterator<KeyOfMC> itr = MCAgent2.selected.keySet().iterator();
		while(itr.hasNext()){
			KeyOfMC key = (KeyOfMC)itr.next();
			MCAgent2.sumValue[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]+= reward;
			MCAgent2.num[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]++;
			MCAgent2.qValue[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()] =
					MCAgent2.sumValue[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]
							/ (float)MCAgent2.num[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()];
		}
		
		return reward;
	}
	//////////////////////////////ここからは必要なし//////////////////////////////
	@Override
	public boolean[] getAction() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void integrateObservation(Environment environment) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void giveIntermediateReward(float intermediateReward) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setObservationDetails(int rfWidth, int rfHeight, int egoRow, int egoCol) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void giveReward(float reward) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void newEpisode() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setLearningTask(LearningTask learningTask) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Agent getBestAgent() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setEvaluationQuota(long num) {
		// TODO Auto-generated method stub
		
	}
}