import { useState, useCallback, useMemo, useEffect } from 'react';
import { CreateNovelRequest } from '../services/api';

export type StepStatus = 'pending' | 'in-progress' | 'completed' | 'skipped';

export interface StepDefinition {
  id: string;
  title: string;
  description: string;
  hasAutoGenerate: boolean;
  placeholder: string;
}

export interface StepState {
  status: StepStatus;
  content: string;
  suggestions?: string[];
  selectedSuggestion?: string;
}

export const STEPS: StepDefinition[] = [
  { id: 'intent', title: '写作意图', description: '请描述你想写的故事', hasAutoGenerate: false, placeholder: '例如：我想写一个主角重生到异世界修仙的故事...' },
  { id: 'genre', title: '赛道选择', description: '选择你的小说类型', hasAutoGenerate: true, placeholder: '基于你的意图，我推荐...' },
  { id: 'synopsis', title: '故事梗概', description: '简要描述故事核心', hasAutoGenerate: true, placeholder: '简单说说你的故事...' },
  { id: 'title', title: '小说标题', description: '为你的小说取个名字', hasAutoGenerate: true, placeholder: '' },
  { id: 'synopsis_v2', title: '小说简介', description: '完善你的简介', hasAutoGenerate: true, placeholder: '' },
  { id: 'power_system', title: '力量体系', description: '设定修炼/能力体系', hasAutoGenerate: true, placeholder: '例如：炼气→筑基→金丹→元婴...' },
  { id: 'world_background', title: '世界背景', description: '描述世界设定', hasAutoGenerate: true, placeholder: '例如：一个修仙为主的世界，有多个大陆...' },
  { id: 'golden_finger', title: '金手指', description: '主角的特殊能力', hasAutoGenerate: true, placeholder: '例如：主角拥有一个神秘的随身空间...' },
  { id: 'total_volumes', title: '预计卷数', description: '计划写多少卷', hasAutoGenerate: true, placeholder: '' },
  { id: 'confirm', title: '信息确认', description: '确认你的设定', hasAutoGenerate: false, placeholder: '' },
];

export interface UseDialogueGuideOptions {
  initialFormData?: Partial<CreateNovelRequest>;
  onFormDataChange?: (data: Partial<CreateNovelRequest>) => void;
}

function formDataToSteps(data: Partial<CreateNovelRequest>): Partial<Record<string, string>> {
  return {
    title: data.title || '',
    genre: data.genre || '',
    synopsis: data.synopsis || '',
    goldenFinger: data.goldenFinger || '',
    totalVolumes: data.totalVolumes ? String(data.totalVolumes) : '',
  };
}

export function useDialogueGuide({ initialFormData, onFormDataChange }: UseDialogueGuideOptions = {}) {
  const initialSteps = useMemo((): StepState[] => {
    const mapped = formDataToSteps(initialFormData || {});
    const steps: StepState[] = STEPS.map(step => {
      const content = mapped[step.id] || '';
      return {
        status: content ? 'completed' : 'pending',
        content,
      };
    });
    // First step is always in-progress if no content
    if (!steps[0]?.content) {
      steps[0].status = 'in-progress';
    }
    return steps;
  }, [initialFormData]);

  const [stepStates, setStepStates] = useState<StepState[]>(initialSteps);
  const [currentStep, setCurrentStep] = useState(() => {
    // Find first non-completed step
    const idx = initialSteps.findIndex(s => s.status !== 'completed');
    return idx >= 0 ? idx : 0;
  });
  const [userInput, setUserInput] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);

  // Sync user input with current step content
  useEffect(() => {
    setUserInput(stepStates[currentStep]?.content || '');
  }, [currentStep, stepStates]);

  const updateStepState = useCallback((stepIndex: number, updates: Partial<StepState>) => {
    setStepStates(prev => {
      const next = [...prev];
      next[stepIndex] = { ...next[stepIndex], ...updates };
      return next;
    });
  }, []);

  const goNext = useCallback(() => {
    if (currentStep < STEPS.length - 1) {
      const nextStep = currentStep + 1;
      // Mark current as completed if has content
      if (userInput.trim()) {
        updateStepState(currentStep, { status: 'completed', content: userInput.trim() });
      }
      setCurrentStep(nextStep);
      // Mark next as in-progress if pending (use functional update to avoid stale closure)
      setStepStates(prev => {
        if (prev[nextStep]?.status === 'pending') {
          const next = [...prev];
          next[nextStep] = { ...next[nextStep], status: 'in-progress' };
          return next;
        }
        return prev;
      });
    }
  }, [currentStep, userInput, updateStepState]);

  const goPrev = useCallback(() => {
    if (currentStep > 0) {
      // Save current input before navigating
      if (userInput.trim()) {
        updateStepState(currentStep, { status: 'completed', content: userInput.trim() });
      }
      const prevStep = currentStep - 1;
      setCurrentStep(prevStep);
      setStepStates(prev => {
        if (prev[prevStep]?.status === 'pending') {
          const next = [...prev];
          next[prevStep] = { ...next[prevStep], status: 'in-progress' };
          return next;
        }
        return prev;
      });
    }
  }, [currentStep, userInput, updateStepState]);

  const goTo = useCallback((stepIndex: number) => {
    if (stepIndex < 0 || stepIndex >= STEPS.length) return;
    // Save current input
    if (userInput.trim()) {
      updateStepState(currentStep, { status: 'completed', content: userInput.trim() });
    }
    setCurrentStep(stepIndex);
    setStepStates(prev => {
      if (prev[stepIndex]?.status === 'pending') {
        const next = [...prev];
        next[stepIndex] = { ...next[stepIndex], status: 'in-progress' };
        return next;
      }
      return prev;
    });
  }, [currentStep, userInput, updateStepState]);

  const skipStep = useCallback(() => {
    updateStepState(currentStep, { status: 'skipped' });
    if (currentStep < STEPS.length - 1) {
      const nextStep = currentStep + 1;
      setCurrentStep(nextStep);
      setStepStates(prev => {
        if (prev[nextStep]?.status === 'pending') {
          const next = [...prev];
          next[nextStep] = { ...next[nextStep], status: 'in-progress' };
          return next;
        }
        return prev;
      });
    }
  }, [currentStep, updateStepState]);

  const setStepContent = useCallback((stepIndex: number, content: string) => {
    updateStepState(stepIndex, { content });
  }, [updateStepState]);

  const setSuggestions = useCallback((stepIndex: number, suggestions: string[]) => {
    updateStepState(stepIndex, { suggestions });
  }, [updateStepState]);

  const selectSuggestion = useCallback((stepIndex: number, suggestion: string) => {
    updateStepState(stepIndex, {
      content: suggestion,
      selectedSuggestion: suggestion,
      status: 'completed',
    });
    setUserInput(suggestion);
  }, [updateStepState]);

  const getFormData = useCallback((): Partial<CreateNovelRequest> => {
    const data: Partial<CreateNovelRequest> = {};
    stepStates.forEach((step, index) => {
      if (step.status === 'completed' && step.content) {
        const stepId = STEPS[index].id;
        switch (stepId) {
          case 'intent':
            break; // intent is not in CreateNovelRequest
          case 'genre':
            data.genre = step.content;
            break;
          case 'synopsis':
          case 'synopsis_v2':
            data.synopsis = step.content;
            break;
          case 'title':
            data.title = step.content;
            break;
          case 'golden_finger':
            data.goldenFinger = step.content;
            break;
          case 'total_volumes':
            data.totalVolumes = parseInt(step.content, 10) || 1;
            break;
        }
      }
    });
    return data;
  }, [stepStates]);

  // Notify parent of data changes
  useEffect(() => {
    const data = getFormData();
    onFormDataChange?.(data);
  }, [stepStates, getFormData, onFormDataChange]);

  const currentStepDef = STEPS[currentStep];
  const currentStepState = stepStates[currentStep];

  return {
    steps: STEPS,
    stepStates,
    currentStep,
    currentStepDef,
    currentStepState,
    userInput,
    setUserInput,
    isGenerating,
    setIsGenerating,
    goNext,
    goPrev,
    goTo,
    skipStep,
    setStepContent,
    setSuggestions,
    selectSuggestion,
    getFormData,
  };
}
