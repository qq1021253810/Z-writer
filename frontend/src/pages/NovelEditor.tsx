import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { novelService, ContinueChapterRequest } from '../services/api';
import { useWorkflowSSE } from '@/services/sse';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ThemeToggle } from '@/components/theme-toggle';
import {
  ArrowLeft, Wrench, AlertCircle, Users, GitBranch, FileText,
  Sparkles, Loader2, BookOpen, ChevronRight, Save, Download,
  List, Target,
} from 'lucide-react';

function NovelEditor() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();
  const [novel, setNovel] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  // 左面板 tab
  const [leftPanel, setLeftPanel] = useState<'outline' | 'characters' | 'foreshadow' | 'tools'>('outline');

  // 续写表单
  const [chapterForm, setChapterForm] = useState<ContinueChapterRequest>({
    novelId: parseInt(novelId || '0'),
    volumeNumber: 1,
    chapterNumber: 1,
    chapterTitle: '',
    chapterOutline: '',
    wordCount: 3000,
  });

  // SSE 流式续写
  const sse = useWorkflowSSE();
  const [streamingText, setStreamingText] = useState('');
  const [chapterContent, setChapterContent] = useState('');
  const contentRef = useRef<HTMLDivElement>(null);

  // 卡文修复
  const [blockForm, setBlockForm] = useState({
    blockDescription: '',
    previousContent: '',
    expectedDirection: '',
    blockType: '通用',
  });
  const [blockResult, setBlockResult] = useState<any>(null);
  const [fixingBlock, setFixingBlock] = useState(false);

  // 工具面板
  const [realTimeWordCount, setRealTimeWordCount] = useState<any>(null);

  // 伏笔管理
  const [foreshadows] = useState<any[]>([]);
  const [newForeshadow, setNewForeshadow] = useState({ setupChapter: 1, clueDescription: '' });

  // 自动保存
  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  // 润色和审计
  const [polishResult, setPolishResult] = useState<any>(null);
  const [reviewResult, setReviewResult] = useState<any>(null);
  const [isPolishing, setIsPolishing] = useState(false);
  const [isReviewing, setIsReviewing] = useState(false);

  // 章节列表
  const [chapters, setChapters] = useState<any[]>([]);
  const [showChapterNav, setShowChapterNav] = useState(false);

  useEffect(() => {
    if (novelId) loadNovel(parseInt(novelId));
  }, [novelId]);

  // SSE 结果处理
  useEffect(() => {
    if (sse.result && typeof sse.result === 'string') {
      setChapterContent(sse.result);
    } else if (sse.result?.content) {
      setChapterContent(sse.result.content);
    }
  }, [sse.result]);

  // 流式 token 拼接
  useEffect(() => {
    if (sse.progressLog.length > 0) {
      const lastProgress = sse.progressLog[sse.progressLog.length - 1];
      if (lastProgress?.data?.token) {
        setStreamingText(prev => prev + lastProgress.data.token);
      }
    }
  }, [sse.progressLog]);

  // SSE 完成时，将流式文本写入正式内容
  useEffect(() => {
    if (sse.status === 'completed' && streamingText) {
      setChapterContent(streamingText);
      setStreamingText('');
    }
  }, [sse.status, streamingText]);

  const loadNovel = async (id: number) => {
    try {
      const data = await novelService.getNovelDetail(id);
      setNovel(data);
    } catch (error) {
      console.error('Failed to load novel:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleContinueChapter = async (e: React.FormEvent) => {
    e.preventDefault();
    setStreamingText('');
    setChapterContent('');

    try {
      await sse.submit('/workflows/continue', {
        novelName: novel?.title || novelId,
        volumeNumber: chapterForm.volumeNumber,
        chapterNumber: chapterForm.chapterNumber,
        chapterTitle: chapterForm.chapterTitle,
        chapterOutline: chapterForm.chapterOutline,
        wordCount: chapterForm.wordCount,
      });
    } catch (error: any) {
      console.error('续写失败:', error);
    }
  };

  const handleFixBlock = async (e: React.FormEvent) => {
    e.preventDefault();
    setFixingBlock(true);
    setBlockResult(null);
    try {
      const response = await fetch('http://localhost:8080/api/workflows/fix-block', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          novelName: novel?.title || novelId,
          problemDescription: blockForm.blockDescription,
          blockType: blockForm.blockType,
          previousContent: blockForm.previousContent,
          expectedDirection: blockForm.expectedDirection,
        }),
      });
      const data = await response.json();
      setBlockResult(data);
    } catch (error) {
      console.error('卡文修复失败:', error);
    } finally {
      setFixingBlock(false);
    }
  };

  const handleSave = async () => {
    if (!novelId || !chapterContent) return;
    setIsSaving(true);
    try {
      await fetch(`http://localhost:8080/api/novels/${novelId}/content`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          volumeNumber: chapterForm.volumeNumber,
          chapterNumber: chapterForm.chapterNumber,
          content: chapterContent,
        }),
      });
      setLastSaved(new Date());
    } catch (error) {
      console.error('保存失败:', error);
    } finally {
      setIsSaving(false);
    }
  };

  // 润色章节
  const handlePolish = async () => {
    if (!novel?.title || !chapterForm.chapterNumber) return;
    setIsPolishing(true);
    setPolishResult(null);
    try {
      const response = await fetch('http://localhost:8080/api/workflows/polish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          novelName: novel.title,
          chapterNum: chapterForm.chapterNumber,
          polishType: 'polish',
        }),
      });
      const data = await response.json();
      setPolishResult(data);
      if (data.success && data.data?.content) {
        setChapterContent(data.data.content);
      }
    } catch (error) {
      console.error('润色失败:', error);
      setPolishResult({ success: false, errorMessage: '润色请求失败' });
    } finally {
      setIsPolishing(false);
    }
  };

  // 审计章节
  const handleReview = async () => {
    if (!novel?.title || !chapterForm.chapterNumber) return;
    setIsReviewing(true);
    setReviewResult(null);
    try {
      const response = await fetch('http://localhost:8080/api/workflows/review', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          novelName: novel.title,
          chapterNum: chapterForm.chapterNumber,
          reviewType: 'quality',
        }),
      });
      const data = await response.json();
      setReviewResult(data);
    } catch (error) {
      console.error('审计失败:', error);
      setReviewResult({ success: false, errorMessage: '审计请求失败' });
    } finally {
      setIsReviewing(false);
    }
  };

  // 实时字数统计
  useEffect(() => {
    const timer = setTimeout(() => {
      if (chapterContent.trim()) {
        const chinese = (chapterContent.match(/[\u4e00-\u9fa5]/g) || []).length;
        const english = (chapterContent.match(/[a-zA-Z]/g) || []).length;
        const total = chapterContent.length;
        setRealTimeWordCount({ totalWords: total, chineseCount: chinese, englishCount: english });
      } else {
        setRealTimeWordCount(null);
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [chapterContent]);

  const handleFormChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setChapterForm(prev => ({
      ...prev,
      [name]: name === 'volumeNumber' || name === 'chapterNumber' || name === 'wordCount'
        ? parseInt(value) : value,
    }));
  };

  // 导出章节
  const handleExportChapter = () => {
    if (!chapterContent) return;
    const blob = new Blob([chapterContent], { type: 'text/markdown' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${novel?.title || 'novel'}-chapter-${chapterForm.chapterNumber}.md`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  // 战略规划
  const [strategyResult, setStrategyResult] = useState<any>(null);
  const [isStrategizing, setIsStrategizing] = useState(false);

  const handleStrategy = async () => {
    if (!novel?.title) return;
    setIsStrategizing(true);
    setStrategyResult(null);
    try {
      const response = await fetch('http://localhost:8080/api/workflows/strategy', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          novelName: novel.title,
          strategyType: 'master_plan',
        }),
      });
      const data = await response.json();
      setStrategyResult(data);
    } catch (error) {
      console.error('战略规划失败:', error);
      setStrategyResult({ success: false, errorMessage: '战略规划失败' });
    } finally {
      setIsStrategizing(false);
    }
  };

  // 加载章节列表
  useEffect(() => {
    if (novel?.title) {
      fetch(`http://localhost:8080/api/novels/${novel.title}/chapters`)
        .then(res => res.json())
        .then(data => {
          if (data.success && Array.isArray(data.data)) {
            setChapters(data.data);
          }
        })
        .catch(err => console.error('加载章节列表失败:', err));
    }
  }, [novel?.title]);

  // 切换章节
  const handleSwitchChapter = async (chapterNum: number) => {
    if (!novel?.title) return;
    try {
      const response = await fetch(`http://localhost:8080/api/novels/${novel.title}/chapters/${chapterNum}`);
      const data = await response.json();
      if (data.success && data.data?.content) {
        setChapterContent(data.data.content);
        setChapterForm(prev => ({ ...prev, chapterNumber: chapterNum }));
        setShowChapterNav(false);
      }
    } catch (error) {
      console.error('加载章节失败:', error);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center space-y-4">
          <Skeleton className="h-12 w-12 rounded-full mx-auto" />
          <p className="text-muted-foreground">加载中...</p>
        </div>
      </div>
    );
  }

  if (!novel) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center">
          <p className="text-muted-foreground">小说不存在</p>
          <Button onClick={() => navigate('/')} className="mt-4" variant="outline">返回首页</Button>
        </div>
      </div>
    );
  }

  const leftPanelTabs = [
    { id: 'outline' as const, label: '大纲', icon: BookOpen },
    { id: 'characters' as const, label: '角色', icon: Users },
    { id: 'foreshadow' as const, label: '伏笔', icon: GitBranch },
    { id: 'tools' as const, label: '工具', icon: Wrench },
  ];

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <div className="border-b border-border">
        <div className="container mx-auto px-4 py-3 flex items-center justify-between max-w-[1600px]">
          <div className="flex items-center gap-4">
            <Button variant="ghost" onClick={() => navigate('/')} className="gap-2">
              <ArrowLeft className="h-4 w-4" />
              返回
            </Button>
            <div>
              <h1 className="text-lg font-medium">{novel.title}</h1>
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Badge variant="outline" className="text-xs">{novel.genre}</Badge>
                {lastSaved && <span>上次保存: {lastSaved.toLocaleTimeString()}</span>}
              </div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={() => setShowChapterNav(!showChapterNav)} disabled={chapters.length === 0}>
              <List className="h-4 w-4 mr-1" />
              章节 ({chapters.length})
            </Button>
            <Button variant="outline" size="sm" onClick={handleExportChapter} disabled={!chapterContent}>
              <Download className="h-4 w-4 mr-1" />
              导出
            </Button>
            <Button variant="outline" size="sm" onClick={handleSave} disabled={!chapterContent || isSaving}>
              {isSaving ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Save className="h-4 w-4 mr-1" />}
              保存
            </Button>
            <ThemeToggle />
          </div>
        </div>
      </div>

      {/* Main: Two-column layout */}
      <div className="container mx-auto px-4 py-4 max-w-[1600px]">
        <div className="grid grid-cols-12 gap-4 h-[calc(100vh-6rem)]">

          {/* Left Panel (4 cols) */}
          <div className="col-span-12 lg:col-span-4 flex flex-col gap-3 overflow-hidden">
            {/* Panel tabs */}
            <div className="flex gap-1 border-b border-border pb-2">
              {leftPanelTabs.map(tab => {
                const Icon = tab.icon;
                return (
                  <button
                    key={tab.id}
                    onClick={() => setLeftPanel(tab.id)}
                    className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                      leftPanel === tab.id
                        ? 'bg-primary text-primary-foreground'
                        : 'text-muted-foreground hover:bg-accent'
                    }`}
                  >
                    <Icon className="h-3.5 w-3.5" />
                    {tab.label}
                  </button>
                );
              })}
            </div>

            {/* Panel content */}
            <div className="flex-1 overflow-y-auto">
              {leftPanel === 'outline' && (
                <Card className="h-full">
                  <CardHeader className="pb-3">
                    <CardTitle className="text-sm">章节设定</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div className="grid grid-cols-2 gap-2">
                      <div>
                        <label className="text-xs text-muted-foreground">卷号</label>
                        <input
                          type="number" name="volumeNumber"
                          value={chapterForm.volumeNumber} onChange={handleFormChange}
                          min={1}
                          className="flex h-8 w-full rounded-md border border-input bg-background px-2 py-1 text-sm"
                        />
                      </div>
                      <div>
                        <label className="text-xs text-muted-foreground">章节号</label>
                        <input
                          type="number" name="chapterNumber"
                          value={chapterForm.chapterNumber} onChange={handleFormChange}
                          min={1}
                          className="flex h-8 w-full rounded-md border border-input bg-background px-2 py-1 text-sm"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="text-xs text-muted-foreground">章节标题</label>
                      <input
                        type="text" name="chapterTitle"
                        value={chapterForm.chapterTitle} onChange={handleFormChange}
                        placeholder="可选，不填则自动生成"
                        className="flex h-8 w-full rounded-md border border-input bg-background px-2 py-1 text-sm"
                      />
                    </div>
                    <div>
                      <label className="text-xs text-muted-foreground">章节大纲</label>
                      <Textarea
                        name="chapterOutline"
                        value={chapterForm.chapterOutline} onChange={handleFormChange}
                        rows={4}
                        placeholder="描述本章主要内容（可选）"
                        className="resize-none"
                      />
                    </div>
                    <div>
                      <label className="text-xs text-muted-foreground">目标字数</label>
                      <input
                        type="number" name="wordCount"
                        value={chapterForm.wordCount} onChange={handleFormChange}
                        min={1000} max={10000} step={500}
                        className="flex h-8 w-full rounded-md border border-input bg-background px-2 py-1 text-sm"
                      />
                    </div>
                    <Button
                      onClick={handleContinueChapter}
                      disabled={sse.isRunning}
                      className="w-full"
                      size="sm"
                    >
                      {sse.isRunning ? (
                        <>
                          <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                          {sse.currentStep || '生成中...'}
                        </>
                      ) : (
                        <>
                          <Sparkles className="h-4 w-4 mr-1" />
                          开始续写
                        </>
                      )}
                    </Button>

                    {/* SSE 进度 */}
                    {sse.isRunning && sse.progressLog.length > 0 && (
                      <div className="space-y-1 mt-2">
                        {sse.progressLog.slice(-3).map((p, i) => (
                          <div key={i} className="text-xs text-muted-foreground flex items-center gap-1">
                            <ChevronRight className="h-3 w-3" />
                            {p.currentStep}
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              )}

              {leftPanel === 'characters' && (
                <Card className="h-full">
                  <CardHeader className="pb-3">
                    <CardTitle className="text-sm">角色信息</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <p className="text-xs text-muted-foreground">
                      角色信息将从工作区上下文自动加载。
                      在创作过程中，系统会自动追踪角色状态变化。
                    </p>
                    <div className="mt-4 p-3 rounded-lg border border-dashed border-border">
                      <p className="text-xs text-muted-foreground italic">
                        角色面板将在后续版本中集成 CharacterTracker 上下文数据。
                      </p>
                    </div>
                  </CardContent>
                </Card>
              )}

              {leftPanel === 'foreshadow' && (
                <Card className="h-full">
                  <CardHeader className="pb-3">
                    <CardTitle className="text-sm">伏笔管理</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div className="flex gap-2">
                      <input
                        type="number"
                        value={newForeshadow.setupChapter}
                        onChange={(e) => setNewForeshadow(prev => ({ ...prev, setupChapter: parseInt(e.target.value) || 1 }))}
                        className="flex h-8 w-16 rounded-md border border-input bg-background px-2 py-1 text-xs"
                        min={1}
                      />
                      <input
                        type="text"
                        value={newForeshadow.clueDescription}
                        onChange={(e) => setNewForeshadow(prev => ({ ...prev, clueDescription: e.target.value }))}
                        className="flex h-8 flex-1 rounded-md border border-input bg-background px-2 py-1 text-xs"
                        placeholder="伏笔描述"
                      />
                    </div>
                    {foreshadows.length > 0 && (
                      <div className="space-y-2">
                        {foreshadows.map((f: any) => (
                          <div key={f.id} className={`p-2 rounded border text-xs ${f.status === 'planted' ? 'border-yellow-200/50 bg-yellow-50/30' : 'border-green-200/50 bg-green-50/30'}`}>
                            <Badge variant={f.status === 'planted' ? 'secondary' : 'default'} className="text-xs">
                              {f.status === 'planted' ? '未回收' : '已回收'}
                            </Badge>
                            <span className="ml-1 text-muted-foreground">第{f.setupChapter}章</span>
                            <p className="mt-1">{f.clueDescription}</p>
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              )}

              {leftPanel === 'tools' && (
                <div className="space-y-3">
                  {/* 卡文修复 */}
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm flex items-center gap-1">
                        <AlertCircle className="h-3.5 w-3.5" />
                        卡文修复
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-2">
                      <Textarea
                        value={blockForm.blockDescription}
                        onChange={(e) => setBlockForm(prev => ({ ...prev, blockDescription: e.target.value }))}
                        rows={2}
                        placeholder="描述卡文问题..."
                        className="text-xs"
                      />
                      <Textarea
                        value={blockForm.previousContent}
                        onChange={(e) => setBlockForm(prev => ({ ...prev, previousContent: e.target.value }))}
                        rows={3}
                        placeholder="前文内容..."
                        className="text-xs"
                      />
                      <Button
                        onClick={handleFixBlock}
                        disabled={fixingBlock || !blockForm.blockDescription.trim()}
                        size="sm"
                        className="w-full"
                      >
                        {fixingBlock ? '修复中...' : '修复卡文'}
                      </Button>
                      {blockResult?.success && (
                        <div className="text-xs p-2 rounded bg-green-50/50 border border-green-200/50 whitespace-pre-wrap">
                          {blockResult.data?.rewrittenContent || blockResult.data?.solutions || '修复完成'}
                        </div>
                      )}
                    </CardContent>
                  </Card>

                  {/* 章节润色 */}
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm flex items-center gap-1">
                        <Sparkles className="h-3.5 w-3.5" />
                        章节润色
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-2">
                      <Button
                        onClick={handlePolish}
                        disabled={isPolishing || !chapterContent}
                        size="sm"
                        className="w-full"
                      >
                        {isPolishing ? '润色中...' : '润色当前章节'}
                      </Button>
                      {polishResult?.success && (
                        <div className="text-xs p-2 rounded bg-blue-50/50 border border-blue-200/50">
                          <p className="font-medium mb-1">润色完成</p>
                          <p className="text-muted-foreground">
                            原文 {polishResult.data?.metadata?.originalLength} 字 → 润色后 {polishResult.data?.metadata?.polishedLength} 字
                          </p>
                        </div>
                      )}
                      {polishResult && !polishResult.success && (
                        <div className="text-xs p-2 rounded bg-red-50/50 border border-red-200/50 text-red-700">
                          {polishResult.errorMessage || '润色失败'}
                        </div>
                      )}
                    </CardContent>
                  </Card>

                  {/* 章节审计 */}
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm flex items-center gap-1">
                        <FileText className="h-3.5 w-3.5" />
                        章节审计
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-2">
                      <Button
                        onClick={handleReview}
                        disabled={isReviewing || !chapterContent}
                        size="sm"
                        className="w-full"
                      >
                        {isReviewing ? '审计中...' : '审计当前章节'}
                      </Button>
                      {reviewResult?.success && (
                        <div className="text-xs p-2 rounded bg-green-50/50 border border-green-200/50 whitespace-pre-wrap max-h-40 overflow-y-auto">
                          <p className="font-medium mb-1">审计报告</p>
                          <div className="prose prose-xs dark:prose-invert max-w-none">
                            {reviewResult.data?.content}
                          </div>
                        </div>
                      )}
                      {reviewResult && !reviewResult.success && (
                        <div className="text-xs p-2 rounded bg-red-50/50 border border-red-200/50 text-red-700">
                          {reviewResult.errorMessage || '审计失败'}
                        </div>
                      )}
                    </CardContent>
                  </Card>

                  {/* 战略规划 */}
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm flex items-center gap-1">
                        <Target className="h-3.5 w-3.5" />
                        战略规划
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-2">
                      <Button
                        onClick={handleStrategy}
                        disabled={isStrategizing}
                        size="sm"
                        className="w-full"
                      >
                        {isStrategizing ? '规划中...' : '生成总体战略'}
                      </Button>
                      {strategyResult?.success && (
                        <div className="text-xs p-2 rounded bg-purple-50/50 border border-purple-200/50 whitespace-pre-wrap max-h-60 overflow-y-auto">
                          <p className="font-medium mb-1">战略规划</p>
                          <div className="prose prose-xs dark:prose-invert max-w-none">
                            {strategyResult.data?.content}
                          </div>
                        </div>
                      )}
                      {strategyResult && !strategyResult.success && (
                        <div className="text-xs p-2 rounded bg-red-50/50 border border-red-200/50 text-red-700">
                          {strategyResult.errorMessage || '战略规划失败'}
                        </div>
                      )}
                    </CardContent>
                  </Card>

                  {/* 字数统计 */}
                  {realTimeWordCount && (
                    <Card>
                      <CardHeader className="pb-3">
                        <CardTitle className="text-sm">实时字数</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="grid grid-cols-3 gap-2 text-center">
                          <div>
                            <p className="text-lg font-semibold">{realTimeWordCount.totalWords}</p>
                            <p className="text-xs text-muted-foreground">总字数</p>
                          </div>
                          <div>
                            <p className="text-lg font-semibold">{realTimeWordCount.chineseCount}</p>
                            <p className="text-xs text-muted-foreground">中文</p>
                          </div>
                          <div>
                            <p className="text-lg font-semibold">{realTimeWordCount.englishCount}</p>
                            <p className="text-xs text-muted-foreground">英文</p>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Right Panel (8 cols) - Content Editor */}
          <div className="col-span-12 lg:col-span-8 flex flex-col gap-3 overflow-hidden">
            <Card className="flex-1 flex flex-col overflow-hidden">
              <CardHeader className="pb-2 flex-shrink-0">
                <div className="flex items-center justify-between">
                  <CardTitle className="text-sm flex items-center gap-2">
                    <FileText className="h-4 w-4" />
                    正文内容
                    {chapterForm.chapterTitle && <span className="text-muted-foreground">— {chapterForm.chapterTitle}</span>}
                  </CardTitle>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    {sse.isRunning && (
                      <Badge variant="outline" className="text-xs animate-pulse">
                        <Loader2 className="h-3 w-3 mr-1 animate-spin" />
                        {sse.currentStep}
                      </Badge>
                    )}
                    {sse.status === 'completed' && (
                      <Badge className="bg-green-500 text-white text-xs">完成</Badge>
                    )}
                    {sse.error && (
                      <Badge variant="destructive" className="text-xs">{sse.error}</Badge>
                    )}
                  </div>
                </div>
              </CardHeader>
              <CardContent className="flex-1 overflow-hidden">
                {/* SSE 流式输出区域 */}
                {sse.isRunning && streamingText && (
                  <div className="mb-3 p-3 rounded-lg border border-primary/30 bg-primary/5 max-h-[200px] overflow-y-auto">
                    <div className="flex items-center gap-1 mb-1">
                      <Sparkles className="h-3 w-3 text-primary animate-pulse" />
                      <span className="text-xs font-medium text-primary">AI 正在创作...</span>
                    </div>
                    <div className="text-sm text-foreground whitespace-pre-wrap leading-relaxed">
                      {streamingText}
                      <span className="inline-block w-0.5 h-4 bg-primary animate-pulse ml-0.5 align-middle" />
                    </div>
                  </div>
                )}

                {/* 主内容编辑区 */}
                <div
                  ref={contentRef}
                  className="h-full overflow-y-auto"
                >
                  {chapterContent ? (
                    <div className="relative group">
                      <Textarea
                        value={chapterContent}
                        onChange={(e) => setChapterContent(e.target.value)}
                        className="min-h-[calc(100vh-16rem)] resize-none text-sm leading-relaxed border-0 p-0 focus-visible:ring-0"
                        placeholder="正文内容将显示在这里，你也可以直接编辑..."
                      />
                    </div>
                  ) : (
                    <div className="flex items-center justify-center h-full text-muted-foreground">
                      <div className="text-center space-y-2">
                        <FileText className="h-12 w-12 mx-auto opacity-20" />
                        <p className="text-sm">
                          {sse.isRunning ? 'AI 正在创作中，请稍候...' : '点击左侧"开始续写"生成章节内容'}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      {/* 章节导航弹窗 */}
      {showChapterNav && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => setShowChapterNav(false)}>
          <div className="bg-card rounded-lg shadow-lg max-w-2xl w-full mx-4 max-h-[80vh] flex flex-col" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between p-4 border-b border-border">
              <h3 className="text-lg font-semibold">章节列表</h3>
              <Button variant="ghost" size="sm" onClick={() => setShowChapterNav(false)}>
                关闭
              </Button>
            </div>
            <div className="flex-1 overflow-y-auto p-4">
              {chapters.length === 0 ? (
                <div className="text-center text-muted-foreground py-8">
                  <FileText className="h-12 w-12 mx-auto opacity-20 mb-2" />
                  <p>暂无章节</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {chapters.map((chapter: any) => (
                    <div
                      key={chapter.chapterNumber}
                      className={`p-3 rounded-lg border cursor-pointer transition-colors ${
                        chapter.chapterNumber === chapterForm.chapterNumber
                          ? 'border-primary bg-primary/10'
                          : 'border-border hover:bg-accent'
                      }`}
                      onClick={() => handleSwitchChapter(chapter.chapterNumber)}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium">第 {chapter.chapterNumber} 章</span>
                          {chapter.title && <span className="text-sm text-muted-foreground">— {chapter.title}</span>}
                        </div>
                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                          <span>{chapter.wordCount} 字</span>
                          {chapter.chapterNumber === chapterForm.chapterNumber && (
                            <Badge variant="default" className="text-xs">当前</Badge>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default NovelEditor;
