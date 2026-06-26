import { useState, useRef, useEffect } from 'react';
import { useSSE } from '@/hooks/useSSE';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Send, Loader2, CheckCircle2 } from 'lucide-react';
import ReactMarkdown from 'react-markdown';

interface DialogueModeProps {
  formData: any;
  onFormDataChange: (data: any) => void;
  onSubmit: () => void;
  loading: boolean;
}

type DialogueStep = 'topic' | 'worldview' | 'characters' | 'outline' | 'complete';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  step?: DialogueStep;
}

export function DialogueMode({ formData, loading }: DialogueModeProps) {
  const [currentStep, setCurrentStep] = useState<DialogueStep>('topic');
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [stepData, setStepData] = useState<Record<string, string>>({});
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const { content: streamingContent, loading: streaming, connect, clear } = useSSE();

  // 自动滚动到最新消息
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, streamingContent]);

  // 处理流式内容完成
  useEffect(() => {
    if (streamingContent && !streaming) {
      setMessages(prev => [...prev, { role: 'assistant', content: streamingContent, step: currentStep }]);
      setStepData(prev => ({ ...prev, [currentStep]: streamingContent }));
      clear();
    }
  }, [streaming, streamingContent, currentStep, clear]);

  const getStepPrompt = (step: DialogueStep): string => {
    switch (step) {
      case 'topic':
        return '请描述你想创作的小说类型和核心概念。例如：都市商战、科幻未来、权力博弈等';
      case 'worldview':
        return '基于你的想法，让我来构建世界观。请补充任何特殊要求，或者回复"继续"让我直接生成';
      case 'characters':
        return '世界观已设定。现在来设计主要角色。请描述主角的核心特质，或者回复"继续"让我生成';
      case 'outline':
        return '角色已就位。接下来规划故事大纲。请说明你期望的故事走向，或者回复"继续"让我生成';
      case 'complete':
        return '所有设定已完成！请检查以上内容，确认无误后点击"开始创作"';
    }
  };

  const handleSend = async () => {
    if (!input.trim()) return;

    const userMessage = input.trim();
    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: userMessage, step: currentStep }]);

    // 根据当前步骤调用不同的 API
    const apiUrl = getApiUrl(currentStep);
    const params = {
      step: currentStep,
      userInput: userMessage,
      existingInfo: stepData,
      ...formData,
    };

    connect(apiUrl, params, 'POST');
  };

  const getApiUrl = (_step: DialogueStep): string => {
    return 'http://localhost:8080/api/dialogue-guide/generate';
  };

  const handleNextStep = () => {
    const steps: DialogueStep[] = ['topic', 'worldview', 'characters', 'outline', 'complete'];
    const currentIndex = steps.indexOf(currentStep);
    if (currentIndex < steps.length - 1) {
      setCurrentStep(steps[currentIndex + 1]);
      setMessages(prev => [...prev, { role: 'assistant', content: getStepPrompt(steps[currentIndex + 1]), step: steps[currentIndex + 1] }]);
    }
  };

  const handleStartOver = () => {
    setCurrentStep('topic');
    setMessages([]);
    setStepData({});
    clear();
  };

  // 初始化对话
  useEffect(() => {
    if (messages.length === 0) {
      setMessages([{ role: 'assistant', content: getStepPrompt('topic'), step: 'topic' }]);
    }
  }, []);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 h-[calc(100vh-200px)]">
      {/* 左侧：对话区 */}
      <Card className="flex flex-col">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <span className="text-lg">对话引导</span>
            <span className="text-xs text-muted-foreground bg-muted px-2 py-1 rounded">
              步骤 {['topic', 'worldview', 'characters', 'outline'].indexOf(currentStep) + 1}/4
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent className="flex-1 flex flex-col overflow-hidden">
          <ScrollArea className="flex-1 pr-4">
            <div className="space-y-4">
              {messages.map((msg, idx) => (
                <div
                  key={idx}
                  className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[80%] rounded-lg px-4 py-2 ${
                      msg.role === 'user'
                        ? 'bg-primary text-primary-foreground'
                        : 'bg-muted'
                    }`}
                  >
                    {msg.role === 'assistant' ? (
                      <div className="prose prose-sm dark:prose-invert max-w-none">
                        <ReactMarkdown>{msg.content}</ReactMarkdown>
                      </div>
                    ) : (
                      <p className="whitespace-pre-wrap">{msg.content}</p>
                    )}
                  </div>
                </div>
              ))}
              {streaming && streamingContent && (
                <div className="flex justify-start">
                  <div className="max-w-[80%] rounded-lg px-4 py-2 bg-muted">
                    <div className="prose prose-sm dark:prose-invert max-w-none">
                      <ReactMarkdown>{streamingContent}</ReactMarkdown>
                    </div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>
          </ScrollArea>

          <div className="mt-4 flex gap-2">
            <Textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={streaming ? '正在生成中...' : '输入你的想法... (Enter 发送, Shift+Enter 换行)'}
              disabled={streaming || loading}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSend();
                }
              }}
              className="min-h-[60px] resize-none"
            />
            <Button
              onClick={handleSend}
              disabled={streaming || loading || !input.trim()}
              className="self-end"
            >
              {streaming ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Send className="h-4 w-4" />
              )}
            </Button>
          </div>

          {currentStep !== 'complete' && stepData[currentStep] && (
            <div className="mt-4 flex gap-2">
              <Button variant="outline" onClick={handleNextStep} className="flex-1">
                下一步
              </Button>
              <Button variant="ghost" onClick={handleStartOver}>
                重新开始
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 右侧：实时预览 */}
      <Card className="flex flex-col">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <span className="text-lg">实时预览</span>
            {Object.keys(stepData).length > 0 && (
              <CheckCircle2 className="h-4 w-4 text-green-500" />
            )}
          </CardTitle>
        </CardHeader>
        <CardContent className="flex-1 overflow-hidden">
          <ScrollArea className="h-full">
            <div className="space-y-6">
              {stepData.topic && (
                <div>
                  <h3 className="font-semibold mb-2 text-primary">选题概念</h3>
                  <div className="prose prose-sm dark:prose-invert max-w-none bg-muted/50 p-4 rounded-lg">
                    <ReactMarkdown>{stepData.topic}</ReactMarkdown>
                  </div>
                </div>
              )}
              {stepData.worldview && (
                <div>
                  <h3 className="font-semibold mb-2 text-primary">世界观设定</h3>
                  <div className="prose prose-sm dark:prose-invert max-w-none bg-muted/50 p-4 rounded-lg">
                    <ReactMarkdown>{stepData.worldview}</ReactMarkdown>
                  </div>
                </div>
              )}
              {stepData.characters && (
                <div>
                  <h3 className="font-semibold mb-2 text-primary">角色设定</h3>
                  <div className="prose prose-sm dark:prose-invert max-w-none bg-muted/50 p-4 rounded-lg">
                    <ReactMarkdown>{stepData.characters}</ReactMarkdown>
                  </div>
                </div>
              )}
              {stepData.outline && (
                <div>
                  <h3 className="font-semibold mb-2 text-primary">故事大纲</h3>
                  <div className="prose prose-sm dark:prose-invert max-w-none bg-muted/50 p-4 rounded-lg">
                    <ReactMarkdown>{stepData.outline}</ReactMarkdown>
                  </div>
                </div>
              )}
              {Object.keys(stepData).length === 0 && (
                <div className="text-center text-muted-foreground py-12">
                  <p>通过左侧对话生成小说设定</p>
                  <p className="text-sm mt-2">生成的内容会在这里实时显示</p>
                </div>
              )}
            </div>
          </ScrollArea>
        </CardContent>
      </Card>
    </div>
  );
}
