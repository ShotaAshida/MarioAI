/*
 * Copyright (c) 2009-2010, Sergey Karakovskiy and Julian Togelius
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Mario AI nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ch.idsia.agents.controllers;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.Environment;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Sergey Karakovskiy, sergey.karakovskiy@gmail.com
 * Date: Apr 8, 2009
 * Time: 4:03:46 AM
 */

public class OwnAgent extends BasicMarioAIAgent implements Agent
{
int trueJumpCounter = 0;
int trueSpeedCounter = 0;
ArrayList< Integer[] > Situation = new ArrayList<Integer[]>();
int ActionCounter = -1;
int Jump = 0;

public OwnAgent()
{
    super("OwnAgent");
    reset();
}

public void reset()
{
    action = new boolean[Environment.numberOfKeys];
    action[Mario.KEY_RIGHT] = true;
    action[Mario.KEY_SPEED] = false;
}

public boolean isObstacle(int r, int c){
	return getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.BRICK
			|| getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH
			|| getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.FLOWER_POT_OR_CANNON
			|| getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.LADDER;
}

public boolean[] getAction()
{
	int y = marioEgoCol;
	int x = marioEgoRow;
	Integer[] E = new Integer[9];
	Arrays.fill(E, 0);
	
	if( isMarioOnGround ){
		ActionCounter++;
		action[Mario.KEY_RIGHT] = true;
		action[Mario.KEY_LEFT] = false;
		action[Mario.KEY_JUMP] = false;
		action[Mario.KEY_SPEED] = false;
		for(int i = 0 ; i < 9 ; i++){
			E[i] = getEnemiesCellValue( x, y+i+1 );
		}
		Situation.add(E);
		Jump = 0;
		for(int j = 0 ; j < 9 ; j++){ 
				if( j < 8 ){
					System.out.print(E[j] + " ");
				}else{
					System.out.println(E[j]);
				}
		}
		
		
		
		
		if( E[0] != 0 || E[1] != 0 && isMarioAbleToJump ){
			action[Mario.KEY_JUMP] = true;
			Jump = 1;
		}
		
		
		
		
	}else{
		int s = 0;
		for(int i = 0; i<9 ; i++){
			if( getReceptiveFieldCellValue(x+i+1, y) != 0 ){
				s = i;
				break;
			}
		}
		for( int i = 0 ; i < 9 ; i++ ){
			E[i] = getEnemiesCellValue( x+s ,y+i+1 );
		}
		Situation.add(E);
		for(int j = 0 ; j < 9 ; j++){ 
			if( j < 8 ){
				System.out.print(E[j] + " ");
			}else{
				System.out.println(E[j]);
			}
		}
		
		
		
		
	}
	
	
	
	
	
	
	
	
	
	
	/*
	if(!isMarioAbleToJump  &&  getEnemiesCellValue(x ,y) != 0 ){
		if( getEnemiesCellValue(x,y-1) != 0 ){
			action[Mario.KEY_JUMP] = true;
			System.out.println("Left1");
		}else{
			action[Mario.KEY_LEFT] = true;
			action[Mario.KEY_RIGHT] = false;
			//action[Mario.KEY_SPEED] = true;
			System.out.println("Left");
		}
	}
	
	if( isObstacle( x, y + 2 ) || isObstacle( x, y + 1 ) ){
		action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;//障害物避ける
		System.out.println("4");
	}
	
	try{
		if(  ((Situation.get( ActionCounter ))[1]  == 0 || (Situation.get(ActionCounter))[2] == 0)
				&& (Situation.get(ActionCounter))[7] != 0 &&  Jump == 1 ){
			action[Mario.KEY_JUMP] = isMarioAbleToJump; 
			action[Mario.KEY_RIGHT] = isMarioAbleToJump; 
			System.out.println("1");
		}
		
	}catch( IndexOutOfBoundsException ex ){
		
	}*/
	
	
    return action;
}
}