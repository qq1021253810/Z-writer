import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Loader2, Sparkles, ChevronLeft, ChevronRight, SkipForward, Check } from 'lucide-react';
import {
  useDialogueGuide,
  StepStatus,
  STEPS,
} from '@/hooks/useDialogueGuide';
import { dialogueGuideService, CreateNovelRequest } from '@/services/api';

interface DialogueModeProps {
  formData: Partial<CreateNovelRequest>;
  onFormDataChange: (data: Partial<CreateNovelRequest>) => void;
  onSubmit?: () => void;
  loading?: boolean;
}

const statusConfig: Record<StepStatus, { label: string; className: string }> = {
  'completed': { label: '已完成', className: 'bg-green-500 border-green-500 text-white hover:bg-green-600' },
  'skipped': { label: '已跳过', className: 'bg-orange-500 border-orange-500 text-white hover:bg-orange-600' },
  'in-progress': { label: '进行中', className: 'bg-blue-500 border-blue-500 text-white hover:bg-blue-600' },
  'pending': { label: '待完成', className: 'bg-gray-400 border-gray-400 text-white' },
};

export function DialogueMode({ formData, onFormDataChange, onSubmit, loading }: DialogueModeProps) {
  const guide = useDialogueGuide({
    initialFormData: formData,
    onFormDataChange,
  });

  const {
    steps,
    stepStates,
    currentStep,
    currentStepDef,
    userInput,
    setUserInput,
    isGenerating,
    setIsGenerating,
    goNext,
    goPrev,
    goTo,
    skipStep,
    selectSuggestion,
  } = guide;

  const handleAutoGenerate = async () => {
    if (!currentStepDef.hasAutoGenerate) return;

    setIsGenerating(true);
    try {
      // Collect existing info from completed steps
      const existingInfo: Record<string, any> = {};
      stepStates.forEach((step, index) => {
        if (step.status === 'completed' && step.content) {
          existingInfo[steps[index].id] = step.content;
        }
      });

      const response = await dialogueGuideService.generate({
        stepType: currentStepDef.id,
        userInput: userInput || undefined,
        existingInfo,
      });

      if (response && response.suggestions) {
        // Store suggestions for display
        guide.setSuggestions(currentStep, Array.isArray(response.suggestions) ? response.suggestions : [response.suggestions]);
      }
    } catch (error) {
      console.error('Failed to generate suggestion:', error);
      // Fallback mock suggestions for demo
      guide.setSuggestions(currentStep, generateMockSuggestions(currentStepDef.id));
    } finally {
      setIsGenerating(false);
    }
  };

  const handleSelectSuggestion = (suggestion: string) => {
    selectSuggestion(currentStep, suggestion);
  };

  const handleConfirmAndNext = () => {
    if (isConfirmStep && onSubmit) {
      onSubmit();
    } else {
      goNext();
    }
  };

  const isConfirmStep = currentStepDef.id === 'confirm';
  const currentStepState = stepStates[currentStep];

  return (
    <div className="grid grid-cols-1 lg:grid-cols-5 gap-6 h-[calc(100vh-12rem)]">
      {/* Left: Dialogue Area (60%) */}
      <div className="lg:col-span-3 flex flex-col gap-4">
        <Card className="flex-1 flex flex-col">
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-sm text-muted-foreground">
                    步骤 {currentStep + 1}/{STEPS.length}
                  </span>
                  <Badge variant={currentStepState.status === 'completed' ? 'default' : 'outline'}>
                    {statusConfig[currentStepState.status].label}
                  </Badge>
                </div>
                <CardTitle className="text-xl">{currentStepDef.title}</CardTitle>
              </div>
            </div>
            <p className="text-muted-foreground mt-1">{currentStepDef.description}</p>
          </CardHeader>

          <CardContent className="flex-1 flex flex-col">
            {/* For confirm step, show summary */}
            {isConfirmStep ? (
              <ConfirmStepView stepStates={stepStates} onEdit={goTo} onSubmit={onSubmit} loading={loading} />
            ) : (
              <>
                {/* Input area */}
                <div className="flex-1">
                  <Textarea
                    value={userInput}
                    onChange={(e) => setUserInput(e.target.value)}
                    placeholder={currentStepDef.placeholder || '请输入内容...'}
                    className="min-h-[150px] resize-none"
                  />

                  {/* Suggestions from auto-generate */}
                  {currentStepState.suggestions && currentStepState.suggestions.length > 0 && (
                    <div className="mt-4 space-y-2">
                      <h4 className="text-sm font-medium text-muted-foreground flex items-center gap-1">
                        <Sparkles className="w-4 h-4" />
                        AI 建议
                      </h4>
                      <div className="space-y-2">
                        {currentStepState.suggestions.map((suggestion, idx) => (
                          <div
                            key={idx}
                            className={`p-3 rounded-lg border cursor-pointer transition-colors ${
                              currentStepState.selectedSuggestion === suggestion
                                ? 'border-primary bg-primary/10'
                                : 'border-border hover:border-primary/50 hover:bg-accent/50'
                            }`}
                            onClick={() => handleSelectSuggestion(suggestion)}
                          >
                            <p className="text-sm">{suggestion}</p>
                          </div>
                        ))}
                      </div>
                      {currentStepState.selectedSuggestion && (
                        <Button
                          className="w-full mt-3"
                          onClick={handleConfirmAndNext}
                        >
                          采用并下一步 <ChevronRight className="w-4 h-4 ml-1" />
                        </Button>
                      )}
                    </div>
                  )}
                </div>

                {/* Action buttons */}
                <div className="flex flex-col gap-3 mt-4 pt-4 border-t">
                  {currentStepDef.hasAutoGenerate && (
                    <Button
                      variant="outline"
                      onClick={handleAutoGenerate}
                      disabled={isGenerating}
                      className="w-full"
                    >
                      {isGenerating ? (
                        <>
                          <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                          生成中...
                        </>
                      ) : (
                        <>
                          <Sparkles className="w-4 h-4 mr-2" />
                          帮我生成
                        </>
                      )}
                    </Button>
                  )}

                  {currentStepState.suggestions && currentStepState.suggestions.length > 0 && !currentStepState.selectedSuggestion && (
                    <Button
                      variant="ghost"
                      onClick={handleAutoGenerate}
                      disabled={isGenerating}
                      size="sm"
                    >
                      <Sparkles className="w-3 h-3 mr-1" />
                      重新生成
                    </Button>
                  )}

                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      onClick={goPrev}
                      disabled={currentStep === 0}
                      size="sm"
                    >
                      <ChevronLeft className="w-4 h-4 mr-1" />
                      上一步
                    </Button>
                    <Button
                      variant="ghost"
                      onClick={skipStep}
                      size="sm"
                    >
                      <SkipForward className="w-4 h-4 mr-1" />
                      跳过
                    </Button>
                    <div className="flex-1" />
                    <Button
                      onClick={goNext}
                      size="sm"
                    >
                      下一步
                      <ChevronRight className="w-4 h-4 ml-1" />
                    </Button>
                  </div>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Right: Preview Panel (40%) */}
      <div className="lg:col-span-2">
        <Card className="h-full">
          <CardHeader>
            <CardTitle className="text-lg">设定进度</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {steps.map((step, index) => {
                const state = stepStates[index];
                const isCurrent = index === currentStep;
                const isClickable = state.status === 'completed' && !isCurrent;

                return (
                  <button
                    key={step.id}
                    className={`w-full text-left p-3 rounded-lg border transition-all ${
                      isCurrent
                        ? 'border-primary bg-primary/5 ring-1 ring-primary/20'
                        : isClickable
                        ? 'border-border hover:border-primary/50 hover:bg-accent/50 cursor-pointer'
                        : 'border-border'
                    }`}
                    onClick={() => isClickable && goTo(index)}
                    disabled={!isClickable}
                  >
                    <div className="flex items-center gap-2">
                      <StatusIcon status={state.status} />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <span className={`text-sm font-medium ${
                            isCurrent ? 'text-primary' : 'text-foreground'
                          }`}>
                            {step.title}
                          </span>
                          {isClickable && (
                            <span className="text-xs text-muted-foreground">点击编辑</span>
                          )}
                        </div>
                        {state.content && (
                          <p className="text-xs text-muted-foreground truncate mt-0.5">
                            {state.content}
                          </p>
                        )}
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function StatusIcon({ status }: { status: StepStatus }) {
  switch (status) {
    case 'completed':
      return <Check className="w-4 h-4 text-green-500 flex-shrink-0" />;
    case 'skipped':
      return <SkipForward className="w-4 h-4 text-orange-500 flex-shrink-0" />;
    case 'in-progress':
      return <Loader2 className="w-4 h-4 text-blue-500 animate-spin flex-shrink-0" />;
    case 'pending':
      return <div className="w-4 h-4 rounded-full border-2 border-gray-300 flex-shrink-0" />;
  }
}

function ConfirmStepView({
  stepStates,
  onEdit,
  onSubmit,
  loading,
}: {
  stepStates: ReturnType<typeof useDialogueGuide>['stepStates'];
  onEdit: (step: number) => void;
  onSubmit?: () => void;
  loading?: boolean;
}) {
  return (
    <div className="space-y-3 flex flex-col h-full">
      <p className="text-sm text-muted-foreground mb-4">
        请确认以下设定信息，确认后即可开始创作。
      </p>
      <div className="flex-1 overflow-y-auto space-y-3">
        {STEPS.filter(s => s.id !== 'confirm').map((step, index) => {
          const state = stepStates[index];
          const hasContent = state.status === 'completed' && state.content;

          return (
            <div key={step.id} className={`p-3 rounded-lg border ${hasContent ? 'bg-card' : 'bg-muted/50'}`}>
              <div className="flex items-center justify-between mb-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium">{step.title}</span>
                  {!hasContent && (
                    <span className="text-xs text-orange-500">待补充</span>
                  )}
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => onEdit(index)}
                  className="h-6 px-2 text-xs"
                >
                  编辑
                </Button>
              </div>
              <p className={`text-sm ${hasContent ? 'text-muted-foreground' : 'text-muted-foreground italic'}`}>
                {hasContent ? state.content : '此项尚未填写'}
              </p>
            </div>
          );
        })}
      </div>
      {onSubmit && (
        <Button
          onClick={onSubmit}
          disabled={loading}
          className="w-full mt-4"
          size="lg"
        >
          {loading ? (
            <>
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              生成中...
            </>
          ) : (
            <>
              <Sparkles className="w-4 h-4 mr-2" />
              开始创作
            </>
          )}
        </Button>
      )}
    </div>
  );
}

// Mock suggestions for demo when API is not available
function generateMockSuggestions(stepType: string): string[] {
  const mockSuggestions: Record<string, string[]> = {
    genre: ['玄幻', '仙侠', '都市', '科幻', '悬疑', '历史'],
    synopsis: [
      '一个平凡少年意外获得神秘传承，从此踏上修仙之路，经历重重磨难，最终成为一代强者。',
      '在末日降临的世界中，主角觉醒特殊能力，带领幸存者重建文明。',
      '主角重生回到十年前，利用前世记忆改变命运，揭开隐藏在都市背后的修真世界。',
    ],
    title: [
      '重生之仙途',
      '逆天魔尊',
      '都市修仙录',
      '万界独尊',
      '永恒剑主',
    ],
    synopsis_v2: [
      '这是一个关于勇气与成长的故事。主角从一无所有的废柴少年，通过不懈努力和机缘巧合，逐步揭开世界的真相，最终站在巅峰。',
      '在强者为尊的世界里，一个被所有人看不起的少年，凭借独特的金手指和坚韧的意志，一步步逆袭成为传奇。',
    ],
    power_system: [
      '炼气→筑基→金丹→元婴→化神→炼虚→合体→大乘→渡劫',
      'F级→E级→D级→C级→B级→A级→S级→SS级→SSS级',
      '觉醒者→超凡者→掌控者→领域者→规则者→造物主',
    ],
    world_background: [
      '一个以修仙为主的世界，分为东荒、南岭、西漠、北原和中州五大区域，每个区域都有独特的修炼文化和势力格局。',
      '地球灵气复苏三百年后，人类与妖兽共存，城市周围遍布危险禁区，人类依靠觉醒者守护文明。',
      '一个被诸神遗弃的位面，天道残缺，修炼之路充满变数，每隔万年就会有一次大劫。',
    ],
    golden_finger: [
      '主角拥有一个随身空间，可以种植灵草、储存物品，空间内时间流速是外界的十倍。',
      '主角脑海中有一部神秘的天书，可以自动推演功法、识别天材地宝，还能预知部分未来。',
      '主角的眼睛发生了异变，可以看到万物的"气运线"，从而避开危险、把握机缘。',
    ],
    total_volumes: ['3卷', '5卷', '8卷', '10卷'],
    intent: [],
    confirm: [],
  };
  return mockSuggestions[stepType] || ['暂无建议'];
}
