import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { novelService, workflowService, ContinueChapterRequest } from '../services/api';
import mermaid from 'mermaid';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ThemeToggle } from '@/components/theme-toggle';
import { ArrowLeft, Wrench, AlertCircle, Users, GitBranch, FileText, CheckCircle } from 'lucide-react';

function NovelEditor() {
  const { novelId } = useParams<{ novelId: string }>();
  const navigate = useNavigate();
  const [novel, setNovel] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [activeSection, setActiveSection] = useState<string>('write');

  // 续写章节表单
  const [chapterForm, setChapterForm] = useState<ContinueChapterRequest>({
    novelId: parseInt(novelId || '0'),
    volumeNumber: 1,
    chapterNumber: 1,
    chapterTitle: '',
    chapterOutline: '',
    wordCount: 3000,
  });

  const [chapterResult, setChapterResult] = useState<any>(null);
  const [generating, setGenerating] = useState(false);

  // 卡文修复表单
  const [blockForm, setBlockForm] = useState({
    novelId: parseInt(novelId || '0'),
    blockDescription: '',
    previousContent: '',
    expectedDirection: '',
    blockType: '通用',
  });
  const [blockResult, setBlockResult] = useState<any>(null);
  const [fixingBlock, setFixingBlock] = useState(false);

  // 工具面板状态
  const [wordCountText, setWordCountText] = useState('');
  const [wordCountResult, setWordCountResult] = useState<any>(null);
  const [bannedWordText, setBannedWordText] = useState('');
  const [bannedWordResult, setBannedWordResult] = useState<any>(null);
  const [checkingBanned, setCheckingBanned] = useState(false);
  const [realTimeWordCount, setRealTimeWordCount] = useState<any>(null);
  const [highlightedText, setHighlightedText] = useState<string>('');

  // 违禁词检测自定义选项
  const [detectOptions, setDetectOptions] = useState({
    checkBannedWords: true,
    checkSensitiveWords: true,
    sensitivityLevel: 'standard',
    customWords: ''
  });
  const [showAdvancedOptions, setShowAdvancedOptions] = useState(false);

  // 角色关系图状态
  const [mermaidCode, setMermaidCode] = useState('');
  const [loadingRelation, setLoadingRelation] = useState(false);
  const mermaidRef = useRef<HTMLDivElement>(null);

  // 伏笔管理状态
  const [foreshadows, setForeshadows] = useState<any[]>([]);
  const [loadingForeshadows, setLoadingForeshadows] = useState(false);
  const [newForeshadow, setNewForeshadow] = useState({ setupChapter: 1, clueDescription: '' });

  // 人设一致性校验状态
  const [consistencyText, setConsistencyText] = useState('');
  const [consistencyResult, setConsistencyResult] = useState<any>(null);
  const [checkingConsistency, setCheckingConsistency] = useState(false);

  // 查重工具状态
  const [plagiarismText, setPlagiarismText] = useState('');
  const [plagiarismCorpus, setPlagiarismCorpus] = useState('');
  const [plagiarismResult, setPlagiarismResult] = useState<any>(null);
  const [checkingPlagiarism, setCheckingPlagiarism] = useState(false);

  // 思维导图状态
  const [mindmapCode, setMindmapCode] = useState('');
  const [loadingMindmap, setLoadingMindmap] = useState(false);
  const mindmapRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (novelId) {
      loadNovel(parseInt(novelId));
    }
  }, [novelId]);

  useEffect(() => {
    if (mermaidCode && mermaidRef.current) {
      mermaid.initialize({ startOnLoad: false, theme: 'default' });
      mermaidRef.current.innerHTML = mermaidCode;
      mermaid.run({
        nodes: [mermaidRef.current],
      }).catch((err) => {
        console.error('Mermaid 渲染失败:', err);
      });
    }
  }, [mermaidCode]);

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
    setGenerating(true);
    setChapterResult(null);

    try {
      const response = await workflowService.continueChapter(chapterForm);
      setChapterResult(response);
    } catch (error: any) {
      console.error('Failed to continue chapter:', error);
      setChapterResult({ success: false, errorMessage: error.message });
    } finally {
      setGenerating(false);
    }
  };

  const handleChapterFormChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setChapterForm(prev => ({
      ...prev,
      [name]: name === 'volumeNumber' || name === 'chapterNumber' || name === 'wordCount'
        ? parseInt(value)
        : value,
    }));
  };

  const handleFixBlock = async (e: React.FormEvent) => {
    e.preventDefault();
    setFixingBlock(true);
    setBlockResult(null);

    try {
      const response = await workflowService.fixWriterBlock(blockForm);
      setBlockResult(response);
    } catch (error: any) {
      console.error('Failed to fix writer block:', error);
      setBlockResult({ success: false, errorMessage: error.message });
    } finally {
      setFixingBlock(false);
    }
  };

  const handleBlockFormChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setBlockForm(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleWordCount = async () => {
    if (!wordCountText.trim()) return;
    try {
      const response = await fetch('http://localhost:8080/api/word-count/text', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: wordCountText }),
      });
      const data = await response.json();
      if (data.success) {
        setWordCountResult(data.data);
      }
    } catch (error) {
      console.error('字数统计失败:', error);
    }
  };

  const handleBannedWordCheck = async () => {
    if (!bannedWordText.trim()) return;
    setCheckingBanned(true);
    try {
      const requestBody: any = { text: bannedWordText };

      if (!detectOptions.checkBannedWords) {
        requestBody.checkBannedWords = false;
      }
      if (!detectOptions.checkSensitiveWords) {
        requestBody.checkSensitiveWords = false;
      }
      if (detectOptions.sensitivityLevel !== 'standard') {
        requestBody.sensitivityLevel = detectOptions.sensitivityLevel;
      }
      if (detectOptions.customWords.trim()) {
        const customWordsArray = detectOptions.customWords
          .split(/[,，\s]+/)
          .map(word => word.trim())
          .filter(word => word.length > 0);
        if (customWordsArray.length > 0) {
          requestBody.customWords = customWordsArray;
        }
      }

      const response = await fetch('http://localhost:8080/api/banned-word/detect', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody),
      });
      const data = await response.json();
      if (data.success) {
        setBannedWordResult(data.data);
        generateHighlightedText(bannedWordText, data.data);
      }
    } catch (error) {
      console.error('违禁词检测失败:', error);
    } finally {
      setCheckingBanned(false);
    }
  };

  useEffect(() => {
    const timer = setTimeout(async () => {
      if (wordCountText.trim()) {
        try {
          const response = await fetch('http://localhost:8080/api/word-count/text', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: wordCountText }),
          });
          const data = await response.json();
          if (data.success) {
            setRealTimeWordCount(data.data);
          }
        } catch (error) {
          console.error('实时字数统计失败:', error);
        }
      } else {
        setRealTimeWordCount(null);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [wordCountText]);

  const generateHighlightedText = (text: string, result: any) => {
    if (!result || !result.bannedWords || result.bannedWords.length === 0) {
      setHighlightedText('');
      return;
    }

    let highlighted = text;
    result.bannedWords.forEach((word: string) => {
      const regex = new RegExp(`(${word})`, 'gi');
      highlighted = highlighted.replace(regex, `<mark class="bg-red-300 text-red-900 font-bold px-1 rounded">$1</mark>`);
    });
    if (result.sensitiveWords) {
      result.sensitiveWords.forEach((word: string) => {
        const regex = new RegExp(`(${word})`, 'gi');
        highlighted = highlighted.replace(regex, `<mark class="bg-yellow-300 text-yellow-900 font-bold px-1 rounded">$1</mark>`);
      });
    }
    setHighlightedText(highlighted);
  };

  const handleReplaceBannedWords = async () => {
    if (!bannedWordText.trim()) return;
    try {
      const response = await fetch('http://localhost:8080/api/banned-word/replace', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: bannedWordText }),
      });
      const data = await response.json();
      if (data.success) {
        setBannedWordText(data.data);
        setHighlightedText('');
        setTimeout(() => handleBannedWordCheck(), 100);
      }
    } catch (error) {
      console.error('替换违禁词失败:', error);
    }
  };

  const handleLoadRelationGraph = async () => {
    if (!novelId) return;
    setLoadingRelation(true);
    try {
      const response = await fetch(`http://localhost:8080/api/character-relation/mermaid/${novelId}`);
      const data = await response.json();
      if (data.success && data.data.mermaidCode) {
        setMermaidCode(data.data.mermaidCode);
      }
    } catch (error) {
      console.error('加载角色关系图失败:', error);
    } finally {
      setLoadingRelation(false);
    }
  };

  const handleLoadForeshadows = async () => {
    if (!novelId) return;
    setLoadingForeshadows(true);
    try {
      const response = await fetch(`http://localhost:8080/api/foreshadow/novel/${novelId}`);
      const data = await response.json();
      if (data.success) {
        setForeshadows(data.data || []);
      }
    } catch (error) {
      console.error('加载伏笔列表失败:', error);
    } finally {
      setLoadingForeshadows(false);
    }
  };

  const handleAddForeshadow = async () => {
    if (!novelId || !newForeshadow.clueDescription.trim()) return;
    try {
      const response = await fetch('http://localhost:8080/api/foreshadow', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          novelId: parseInt(novelId),
          setupChapter: newForeshadow.setupChapter,
          clueDescription: newForeshadow.clueDescription,
        }),
      });
      const data = await response.json();
      if (data.success) {
        setNewForeshadow({ setupChapter: 1, clueDescription: '' });
        handleLoadForeshadows();
      }
    } catch (error) {
      console.error('添加伏笔失败:', error);
    }
  };

  const handleResolveForeshadow = async (id: number, payoffChapter: number) => {
    try {
      const response = await fetch(`http://localhost:8080/api/foreshadow/${id}/resolve`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ payoffChapter }),
      });
      const data = await response.json();
      if (data.success) {
        handleLoadForeshadows();
      }
    } catch (error) {
      console.error('回收伏笔失败:', error);
    }
  };

  const handleConsistencyCheck = async () => {
    if (!novelId || !consistencyText.trim()) return;
    setCheckingConsistency(true);
    try {
      const response = await fetch('http://localhost:8080/api/consistency/check', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ novelId: parseInt(novelId), text: consistencyText }),
      });
      const data = await response.json();
      if (data.success) {
        setConsistencyResult(data.data);
      }
    } catch (error) {
      console.error('人设一致性校验失败:', error);
    } finally {
      setCheckingConsistency(false);
    }
  };

  const handlePlagiarismCheck = async () => {
    if (!plagiarismText.trim()) return;
    setCheckingPlagiarism(true);
    try {
      const corpus = plagiarismCorpus.trim()
        ? plagiarismCorpus.split('\n').filter(s => s.trim())
        : [];
      const response = await fetch('http://localhost:8080/api/plagiarism/detect', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: plagiarismText, corpus }),
      });
      const data = await response.json();
      if (data.success) {
        setPlagiarismResult(data.data);
      }
    } catch (error) {
      console.error('查重检测失败:', error);
    } finally {
      setCheckingPlagiarism(false);
    }
  };

  const handleLoadMindmap = async () => {
    if (!novelId) return;
    setLoadingMindmap(true);
    try {
      const response = await fetch(`http://localhost:8080/api/mindmap/mermaid/${novelId}`);
      const data = await response.json();
      if (data.success && data.data) {
        setMindmapCode(data.data);
      }
    } catch (error) {
      console.error('加载思维导图失败:', error);
    } finally {
      setLoadingMindmap(false);
    }
  };

  useEffect(() => {
    if (mindmapCode && mindmapRef.current) {
      mermaid.initialize({ startOnLoad: false, theme: 'default' });
      mindmapRef.current.innerHTML = mindmapCode;
      mermaid.run({ nodes: [mindmapRef.current] }).catch((err) => {
        console.error('思维导图渲染失败:', err);
      });
    }
  }, [mindmapCode]);

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
          <Button
            onClick={() => navigate('/')}
            className="mt-4"
            variant="outline"
          >
            返回首页
          </Button>
        </div>
      </div>
    );
  }

  const sections = [
    { id: 'write', label: '续写', icon: FileText },
    { id: 'block', label: '卡文修复', icon: AlertCircle },
    { id: 'tools', label: '工具', icon: Wrench },
  ];

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        <div className="mb-8 flex justify-between items-center">
          <Button
            variant="ghost"
            onClick={() => navigate('/')}
            className="gap-2"
          >
            <ArrowLeft className="h-4 w-4" />
            返回
          </Button>
          <ThemeToggle />
        </div>

        <div className="mb-8">
          <h1 className="text-3xl font-light tracking-wide mb-2">{novel.title}</h1>
          <div className="flex items-center gap-3 text-sm text-muted-foreground">
            <Badge variant="outline">{novel.genre}</Badge>
            <Badge variant="secondary">{novel.status}</Badge>
          </div>
        </div>

        <div className="flex gap-2 mb-8 border-b border-border/50">
          {sections.map((section) => {
            const Icon = section.icon;
            return (
              <button
                key={section.id}
                onClick={() => setActiveSection(section.id)}
                className={`flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors border-b-2 ${
                  activeSection === section.id
                    ? 'border-primary text-primary'
                    : 'border-transparent text-muted-foreground hover:text-foreground'
                }`}
              >
                <Icon className="h-4 w-4" />
                {section.label}
              </button>
            );
          })}
        </div>

        {activeSection === 'write' && (
          <div className="space-y-6">
            <form onSubmit={handleContinueChapter} className="space-y-6">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-foreground mb-2">
                    卷号
                  </label>
                  <input
                    type="number"
                    name="volumeNumber"
                    value={chapterForm.volumeNumber}
                    onChange={handleChapterFormChange}
                    min={1}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-foreground mb-2">
                    章节号
                  </label>
                  <input
                    type="number"
                    name="chapterNumber"
                    value={chapterForm.chapterNumber}
                    onChange={handleChapterFormChange}
                    min={1}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground mb-2">
                  章节标题
                </label>
                <input
                  type="text"
                  name="chapterTitle"
                  value={chapterForm.chapterTitle}
                  onChange={handleChapterFormChange}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  placeholder="可选，不填则自动生成"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground mb-2">
                  章节大纲
                </label>
                <Textarea
                  name="chapterOutline"
                  value={chapterForm.chapterOutline}
                  onChange={handleChapterFormChange}
                  rows={4}
                  placeholder="描述本章主要内容（可选）"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground mb-2">
                  目标字数
                </label>
                <input
                  type="number"
                  name="wordCount"
                  value={chapterForm.wordCount}
                  onChange={handleChapterFormChange}
                  min={1000}
                  max={10000}
                  step={500}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                />
              </div>

              <Button
                type="submit"
                disabled={generating}
                className="w-full"
                size="lg"
              >
                {generating ? '生成中...' : '开始续写'}
              </Button>
            </form>

            {chapterResult && (
              <div className={`p-6 rounded-lg border ${chapterResult.success ? 'border-green-500/30 bg-green-50/30' : 'border-destructive/30 bg-destructive/5'}`}>
                <h3 className="text-xl font-medium mb-4">
                  {chapterResult.success ? '生成成功' : '生成失败'}
                </h3>

                {chapterResult.success ? (
                  <div className="space-y-4">
                    <div className="bg-background/50 p-4 rounded border border-border/50">
                      <h4 className="font-medium mb-2 text-sm text-muted-foreground">生成内容:</h4>
                      <div className="whitespace-pre-wrap text-sm leading-relaxed">
                        {chapterResult.content}
                      </div>
                    </div>
                    <p className="text-sm text-muted-foreground">
                      字数: {chapterResult.wordCount} | 耗时: {chapterResult.durationMs}ms
                    </p>
                  </div>
                ) : (
                  <p className="text-destructive/90">错误: {chapterResult.errorMessage}</p>
                )}
              </div>
            )}
          </div>
        )}

        {activeSection === 'block' && (
          <div className="space-y-6">
            <form onSubmit={handleFixBlock} className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-foreground mb-2">
                  卡文类型
                </label>
                <select
                  name="blockType"
                  value={blockForm.blockType}
                  onChange={handleBlockFormChange}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <option value="通用">通用</option>
                  <option value="情节">情节</option>
                  <option value="人物">人物</option>
                  <option value="节奏">节奏</option>
                  <option value="逻辑">逻辑</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground mb-2">
                  卡文描述 *
                </label>
                <Textarea
                  name="blockDescription"
                  value={blockForm.blockDescription}
                  onChange={handleBlockFormChange}
                  rows={3}
                  required
                  placeholder="描述你遇到的问题，例如：不知道如何推进情节、人物行为不合理等"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground mb-2">
                  前文内容 *
                </label>
                <Textarea
                  name="previousContent"
                  value={blockForm.previousContent}
                  onChange={handleBlockFormChange}
                  rows={6}
                  required
                  placeholder="粘贴卡文位置之前的内容"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground mb-2">
                  期望方向
                </label>
                <Textarea
                  name="expectedDirection"
                  value={blockForm.expectedDirection}
                  onChange={handleBlockFormChange}
                  rows={3}
                  placeholder="你希望情节如何发展（可选）"
                />
              </div>

              <Button
                type="submit"
                disabled={fixingBlock}
                className="w-full"
                size="lg"
              >
                {fixingBlock ? '修复中...' : '开始修复'}
              </Button>
            </form>

            {blockResult && (
              <div className={`p-6 rounded-lg border ${blockResult.success ? 'border-green-500/30 bg-green-50/30' : 'border-destructive/30 bg-destructive/5'}`}>
                <h3 className="text-xl font-medium mb-4">
                  {blockResult.success ? '修复成功' : '修复失败'}
                </h3>

                {blockResult.success ? (
                  <div className="space-y-4">
                    <div className="bg-background/50 p-4 rounded border border-border/50">
                      <h4 className="font-medium mb-2 text-sm text-muted-foreground">问题分析:</h4>
                      <div className="whitespace-pre-wrap text-sm leading-relaxed">
                        {blockResult.analysis}
                      </div>
                    </div>

                    <div className="bg-background/50 p-4 rounded border border-border/50">
                      <h4 className="font-medium mb-2 text-sm text-muted-foreground">解决方案:</h4>
                      <div className="whitespace-pre-wrap text-sm leading-relaxed">
                        {blockResult.solutions}
                      </div>
                    </div>

                    <div className="bg-background/50 p-4 rounded border border-border/50">
                      <h4 className="font-medium mb-2 text-sm text-muted-foreground">修改后内容:</h4>
                      <div className="whitespace-pre-wrap text-sm leading-relaxed">
                        {blockResult.rewrittenContent}
                      </div>
                    </div>

                    <p className="text-sm text-muted-foreground">
                      耗时: {blockResult.durationMs}ms
                    </p>
                  </div>
                ) : (
                  <p className="text-destructive/90">错误: {blockResult.errorMessage}</p>
                )}
              </div>
            )}
          </div>
        )}

        {activeSection === 'tools' && (
          <div className="space-y-8">
            {/* 字数统计工具 */}
            <div>
              <h3 className="text-lg font-medium mb-4 flex items-center gap-2">
                <FileText className="h-5 w-5" />
                字数统计
              </h3>
              <div className="space-y-3">
                <Textarea
                  value={wordCountText}
                  onChange={(e) => setWordCountText(e.target.value)}
                  rows={6}
                  placeholder="粘贴需要统计的文本..."
                />
                <Button onClick={handleWordCount}>
                  统计字数
                </Button>
                {(wordCountResult || realTimeWordCount) && (
                  <div className="p-4 bg-muted/30 rounded-lg border border-border/50">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-sm text-muted-foreground">总字数</p>
                        <p className="text-2xl font-semibold">{(wordCountResult || realTimeWordCount).totalWords}</p>
                      </div>
                      <div>
                        <p className="text-sm text-muted-foreground">中文字数</p>
                        <p className="text-2xl font-semibold">{(wordCountResult || realTimeWordCount).chineseCount}</p>
                      </div>
                      <div>
                        <p className="text-sm text-muted-foreground">英文字母</p>
                        <p className="text-2xl font-semibold">{(wordCountResult || realTimeWordCount).englishCount}</p>
                      </div>
                      <div>
                        <p className="text-sm text-muted-foreground">数字</p>
                        <p className="text-2xl font-semibold">{(wordCountResult || realTimeWordCount).numberCount}</p>
                      </div>
                    </div>
                    {realTimeWordCount && !wordCountResult && (
                      <p className="text-xs text-muted-foreground mt-2">* 实时统计（输入时自动更新）</p>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* 违禁词检测工具 */}
            <div>
              <h3 className="text-lg font-medium mb-4 flex items-center gap-2">
                <AlertCircle className="h-5 w-5" />
                违禁词检测
              </h3>
              <div className="space-y-3">
                <Textarea
                  value={bannedWordText}
                  onChange={(e) => setBannedWordText(e.target.value)}
                  rows={6}
                  placeholder="粘贴需要检测的文本..."
                />

                <div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setShowAdvancedOptions(!showAdvancedOptions)}
                    className="gap-1"
                  >
                    <span>{showAdvancedOptions ? '▼' : '▶'}</span>
                    高级选项
                  </Button>
                  {showAdvancedOptions && (
                    <div className="mt-2 p-4 bg-muted/30 rounded-lg border border-border/50 space-y-3">
                      <div className="flex items-center gap-4">
                        <label className="flex items-center gap-2 text-sm">
                          <input
                            type="checkbox"
                            checked={detectOptions.checkBannedWords}
                            onChange={(e) => setDetectOptions(prev => ({ ...prev, checkBannedWords: e.target.checked }))}
                            className="rounded"
                          />
                          检测违禁词
                        </label>
                        <label className="flex items-center gap-2 text-sm">
                          <input
                            type="checkbox"
                            checked={detectOptions.checkSensitiveWords}
                            onChange={(e) => setDetectOptions(prev => ({ ...prev, checkSensitiveWords: e.target.checked }))}
                            className="rounded"
                          />
                          检测敏感词
                        </label>
                      </div>
                      <div>
                        <label className="block text-sm text-muted-foreground mb-1">敏感度级别</label>
                        <select
                          value={detectOptions.sensitivityLevel}
                          onChange={(e) => setDetectOptions(prev => ({ ...prev, sensitivityLevel: e.target.value }))}
                          className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          <option value="strict">严格（敏感词也视为高风险）</option>
                          <option value="standard">标准（默认）</option>
                          <option value="loose">宽松（敏感词为低风险）</option>
                        </select>
                      </div>
                      <div>
                        <label className="block text-sm text-muted-foreground mb-1">自定义词库（逗号或空格分隔）</label>
                        <input
                          type="text"
                          value={detectOptions.customWords}
                          onChange={(e) => setDetectOptions(prev => ({ ...prev, customWords: e.target.value }))}
                          className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                          placeholder="例如：自定义词 1，自定义词 2，自定义词 3"
                        />
                      </div>
                    </div>
                  )}
                </div>

                <div className="flex gap-2">
                  <Button
                    onClick={handleBannedWordCheck}
                    disabled={checkingBanned}
                  >
                    {checkingBanned ? '检测中...' : '检测违禁词'}
                  </Button>
                  {bannedWordResult && !bannedWordResult.isCompliant && (
                    <Button
                      variant="destructive"
                      onClick={handleReplaceBannedWords}
                    >
                      一键替换违禁词
                    </Button>
                  )}
                </div>

                {highlightedText && (
                  <div className="p-4 bg-muted/30 rounded-lg border border-border/50">
                    <p className="text-sm text-muted-foreground mb-2 font-medium">高亮显示（违禁词红色，敏感词黄色）：</p>
                    <div
                      className="whitespace-pre-wrap text-sm leading-relaxed"
                      dangerouslySetInnerHTML={{ __html: highlightedText }}
                    />
                  </div>
                )}

                {bannedWordResult && (
                  <div className={`p-4 rounded-lg border ${bannedWordResult.isCompliant ? 'border-green-500/30 bg-green-50/30' : 'border-red-500/30 bg-red-50/30'}`}>
                    <div className="mb-3">
                      <p className="text-sm text-muted-foreground">风险等级</p>
                      <p className={`text-xl font-semibold ${
                        bannedWordResult.riskLevel === '低风险' ? 'text-green-600' :
                        bannedWordResult.riskLevel === '中风险' ? 'text-yellow-600' : 'text-red-600'
                      }`}>
                        {bannedWordResult.riskLevel}
                      </p>
                    </div>
                    {bannedWordResult.bannedWords && bannedWordResult.bannedWords.length > 0 && (
                      <div className="mb-3">
                        <p className="text-sm text-muted-foreground mb-1">违禁词:</p>
                        <div className="flex flex-wrap gap-2">
                          {bannedWordResult.bannedWords.map((word: string, idx: number) => (
                            <Badge key={idx} variant="destructive">{word}</Badge>
                          ))}
                        </div>
                      </div>
                    )}
                    {bannedWordResult.sensitiveWords && bannedWordResult.sensitiveWords.length > 0 && (
                      <div>
                        <p className="text-sm text-muted-foreground mb-1">敏感词:</p>
                        <div className="flex flex-wrap gap-2">
                          {bannedWordResult.sensitiveWords.map((word: string, idx: number) => (
                            <Badge key={idx} variant="secondary" className="bg-yellow-200 text-yellow-800">{word}</Badge>
                          ))}
                        </div>
                      </div>
                    )}
                    {bannedWordResult.isCompliant && (
                      <p className="text-green-600 font-medium">内容合规，未检测到违禁词</p>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* 角色关系图 */}
            <div>
              <h3 className="text-lg font-medium mb-4 flex items-center gap-2">
                <Users className="h-5 w-5" />
                角色关系图
              </h3>
              <div className="space-y-4">
                <Button
                  onClick={handleLoadRelationGraph}
                  disabled={loadingRelation}
                >
                  {loadingRelation ? '加载中...' : '加载角色关系图'}
                </Button>
                {mermaidCode && (
                  <div className="bg-background/50 p-4 rounded-lg border border-border/50 overflow-auto">
                    <div ref={mermaidRef} className="mermaid-diagram"></div>
                  </div>
                )}
              </div>
            </div>

            {/* 伏笔管理 */}
            <div>
              <h3 className="text-lg font-medium mb-4 flex items-center gap-2">
                <GitBranch className="h-5 w-5" />
                伏笔管理
              </h3>
              <div className="space-y-3">
                <div className="flex gap-2">
                  <input
                    type="number"
                    value={newForeshadow.setupChapter}
                    onChange={(e) => setNewForeshadow(prev => ({ ...prev, setupChapter: parseInt(e.target.value) || 1 }))}
                    className="flex h-10 w-24 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                    placeholder="埋设章节"
                    min={1}
                  />
                  <input
                    type="text"
                    value={newForeshadow.clueDescription}
                    onChange={(e) => setNewForeshadow(prev => ({ ...prev, clueDescription: e.target.value }))}
                    className="flex h-10 flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                    placeholder="伏笔描述（如：主角母亲留下的玉佩暗藏上古血脉）"
                  />
                  <Button
                    onClick={handleAddForeshadow}
                    size="sm"
                  >
                    添加
                  </Button>
                </div>
                <Button
                  variant="outline"
                  onClick={handleLoadForeshadows}
                  disabled={loadingForeshadows}
                  size="sm"
                >
                  {loadingForeshadows ? '加载中...' : '刷新伏笔列表'}
                </Button>
                {foreshadows.length > 0 && (
                  <div className="space-y-2">
                    {foreshadows.map((f: any) => (
                      <div key={f.id} className={`p-3 rounded-lg border ${f.status === 'planted' ? 'bg-yellow-50/50 border-yellow-200/50' : 'bg-green-50/50 border-green-200/50'}`}>
                        <div className="flex items-center justify-between">
                          <div>
                            <Badge variant={f.status === 'planted' ? 'secondary' : 'default'} className={f.status === 'planted' ? 'bg-yellow-200 text-yellow-800' : 'bg-green-200 text-green-800'}>
                              {f.status === 'planted' ? '未回收' : '已回收'}
                            </Badge>
                            <span className="ml-2 text-sm text-muted-foreground">第{f.setupChapter}章埋设</span>
                            {f.payoffChapter && <span className="text-sm text-muted-foreground"> → 第{f.payoffChapter}章回收</span>}
                          </div>
                          {f.status === 'planted' && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                const ch = prompt('请输入回收章节号：');
                                if (ch) handleResolveForeshadow(f.id, parseInt(ch));
                              }}
                            >
                              标记回收
                            </Button>
                          )}
                        </div>
                        <p className="mt-1 text-sm">{f.clueDescription}</p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* 人设一致性校验 */}
            <div>
              <h3 className="text-lg font-medium mb-4 flex items-center gap-2">
                <CheckCircle className="h-5 w-5" />
                人设一致性校验
              </h3>
              <div className="space-y-3">
                <Textarea
                  value={consistencyText}
                  onChange={(e) => setConsistencyText(e.target.value)}
                  rows={4}
                  placeholder="粘贴需要校验的文本，检查角色对话、行为、战力是否符合人设..."
                />
                <Button
                  onClick={handleConsistencyCheck}
                  disabled={checkingConsistency}
                >
                  {checkingConsistency ? '校验中...' : '校验人设一致性'}
                </Button>
                {consistencyResult && (
                  <div className={`p-4 rounded-lg border ${consistencyResult.isConsistent ? 'border-green-500/30 bg-green-50/30' : 'border-red-500/30 bg-red-50/30'}`}>
                    <p className={`font-medium ${consistencyResult.isConsistent ? 'text-green-600' : 'text-red-600'}`}>
                      {consistencyResult.isConsistent ? '人设一致性校验通过' : `发现 ${consistencyResult.violations?.length || 0} 处人设违规`}
                    </p>
                    {consistencyResult.violations?.map((v: any, idx: number) => (
                      <div key={idx} className="mt-2 p-3 bg-background/50 rounded border border-border/50">
                        <p className="text-sm font-medium text-red-700">
                          [{v.type === 'dialogue' ? '对话' : v.type === 'behavior' ? '行为' : '战力'}] {v.characterName}
                        </p>
                        <p className="text-sm text-muted-foreground">{v.description}</p>
                        <p className="text-sm text-primary">建议：{v.suggestion}</p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* 查重工具 */}
            <div>
              <h3 className="text-lg font-medium mb-4 flex items-center gap-2">
                <FileText className="h-5 w-5" />
                查重检测
              </h3>
              <div className="space-y-3">
                <Textarea
                  value={plagiarismText}
                  onChange={(e) => setPlagiarismText(e.target.value)}
                  rows={4}
                  placeholder="粘贴需要查重的文本..."
                />
                <Textarea
                  value={plagiarismCorpus}
                  onChange={(e) => setPlagiarismCorpus(e.target.value)}
                  rows={3}
                  placeholder="对比语料（每行一段，可选）"
                />
                <Button
                  onClick={handlePlagiarismCheck}
                  disabled={checkingPlagiarism}
                >
                  {checkingPlagiarism ? '检测中...' : '查重检测'}
                </Button>
                {plagiarismResult && (
                  <div className="p-4 bg-muted/30 rounded-lg border border-border/50">
                    <div className="flex items-center gap-4 mb-3">
                      <div>
                        <p className="text-sm text-muted-foreground">总体相似度</p>
                        <p className={`text-2xl font-semibold ${
                          plagiarismResult.overallSimilarity >= 0.6 ? 'text-red-600' :
                          plagiarismResult.overallSimilarity >= 0.3 ? 'text-yellow-600' : 'text-green-600'
                        }`}>
                          {(plagiarismResult.overallSimilarity * 100).toFixed(1)}%
                        </p>
                      </div>
                      <div>
                        <p className="text-sm text-muted-foreground">风险等级</p>
                        <Badge variant={
                          plagiarismResult.riskLevel === '高风险' ? 'destructive' :
                          plagiarismResult.riskLevel === '中风险' ? 'secondary' : 'default'
                        }>
                          {plagiarismResult.riskLevel}
                        </Badge>
                      </div>
                    </div>
                    {plagiarismResult.similarSegments?.length > 0 && (
                      <div className="space-y-2">
                        <p className="text-sm font-medium text-foreground">相似片段：</p>
                        {plagiarismResult.similarSegments.map((seg: any, idx: number) => (
                          <div key={idx} className="p-3 bg-background/50 rounded border border-border/50 text-sm">
                            <p className="text-muted-foreground">原文：{seg.sourceText}</p>
                            <p className="text-primary">匹配：{seg.matchedText}</p>
                            <p className="text-muted-foreground">相似度：{(seg.similarity * 100).toFixed(1)}%</p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* 思维导图导出 */}
            <div>
              <h3 className="text-lg font-medium mb-4 flex items-center gap-2">
                <GitBranch className="h-5 w-5" />
                大纲思维导图
              </h3>
              <div className="space-y-4">
                <Button
                  onClick={handleLoadMindmap}
                  disabled={loadingMindmap}
                >
                  {loadingMindmap ? '加载中...' : '生成思维导图'}
                </Button>
                {mindmapCode && (
                  <div className="bg-background/50 p-4 rounded-lg border border-border/50 overflow-auto">
                    <div ref={mindmapRef} className="mindmap-diagram"></div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default NovelEditor;
