package com.wxy.aiqa.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wxy.aiqa.model.dto.question.QuestionContentDTO;
import com.wxy.aiqa.model.entity.App;
import com.wxy.aiqa.model.entity.Question;
import com.wxy.aiqa.model.entity.ScoringResult;
import com.wxy.aiqa.model.entity.UserAnswer;
import com.wxy.aiqa.model.vo.QuestionVO;
import com.wxy.aiqa.service.AppService;
import com.wxy.aiqa.service.QuestionService;
import com.wxy.aiqa.service.ScoringResultService;

import javax.annotation.Resource;
import java.sql.Wrapper;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义测评类应用评分策略
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 0)
public class CustomScoreScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        // 1.根据id查询题目和题目结果信息
        Long appId = app.getId();
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
        );
        List<ScoringResult> scoringResultsList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class).eq(ScoringResult::getAppId, appId)
        );
        //2.统计用户每个选择对应的属性个数
        Map<String, Integer> optionCount = new HashMap<>();

        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

        for (QuestionContentDTO questionContentDTO : questionContent) {
            for (String answer : choices) {
                for (QuestionContentDTO.Option option : questionContentDTO.getOptions()) {
                    if (option.getKey().equals(answer)) {
                        String result = option.getResult();

                        if (!optionCount.containsKey(result)) {
                            optionCount.put(result, 0);
                        }

                        optionCount.put(result, optionCount.get(result) + 1);
                    }
                }
            }
        }

        //3.遍历每种评分结果，计算哪个结果的得分最高
        int maxScore = 0;
        ScoringResult maxScoringResult = scoringResultsList.get(0);

        for (ScoringResult scoringResult : scoringResultsList) {
            List<String> resultProp = JSONUtil.toList(scoringResult.getResultProp(), String.class);
            int score = resultProp.stream().mapToInt(prop -> optionCount.getOrDefault(prop, 0)).sum();

            if (score > maxScore) {
                maxScore = score;
                maxScoringResult = scoringResult;
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
        return userAnswer;
    }
}
