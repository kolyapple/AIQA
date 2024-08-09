package com.wxy.aiqa.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wxy.aiqa.model.dto.question.QuestionContentDTO;
import com.wxy.aiqa.model.entity.App;
import com.wxy.aiqa.model.entity.Question;
import com.wxy.aiqa.model.entity.ScoringResult;
import com.wxy.aiqa.model.entity.UserAnswer;
import com.wxy.aiqa.model.vo.QuestionVO;
import com.wxy.aiqa.service.QuestionService;
import com.wxy.aiqa.service.ScoringResultService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * 自定义得分类评分策略
 */
@ScoringStrategyConfig(appType = 0, scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        //1.根据id查询到题目和题目结果信息（按分数降序排序）
        Long appId = app.getId();
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
        );
        List<ScoringResult> scoringResultsList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class).eq(ScoringResult::getAppId, appId).orderByDesc(ScoringResult::getResultScoreRange)
        );
        //2.统计用户的总得分
        int totalScore = 0;

        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

        for (QuestionContentDTO questionContentDTO : questionContent) {
            for (String answer : choices) {
                for (QuestionContentDTO.Option option : questionContentDTO.getOptions()) {
                    if (option.getKey().equals(answer)) {
                        int score = Optional.of(option.getScore()).orElse(0);
                        totalScore += score;
                    }
                }
            }
        }
        //3.遍历得分结果，找到第一个大于用户总得分的题目结果，返回该结果
        ScoringResult maxScoringResult = scoringResultsList.get(0);

        for (ScoringResult scoringResult : scoringResultsList) {
            if (totalScore >= scoringResult.getResultScoreRange()) {
                maxScoringResult = scoringResult;
                break;
            }
        }
        //4.构造返回值，填充答案对象的属性
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoringResult.getId());
        userAnswer.setResultName(maxScoringResult.getResultName());
        userAnswer.setResultDesc(maxScoringResult.getResultDesc());
        userAnswer.setResultPicture(maxScoringResult.getResultPicture());
        userAnswer.setResultScore(totalScore);
        return userAnswer;
    }
}
