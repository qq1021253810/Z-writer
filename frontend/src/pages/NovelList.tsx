import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { novelService } from '../services/api';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ThemeToggle } from '@/components/theme-toggle';
import { Pencil, Trash2, Plus, BookOpen, Clock, TrendingUp } from 'lucide-react';

interface Novel {
  id: number;
  title: string;
  genre: string;
  synopsis: string;
  status: string;
  createdAt: string;
}

function NovelList() {
  const navigate = useNavigate();
  const [novels, setNovels] = useState<Novel[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadNovels();
  }, []);

  const loadNovels = async () => {
    try {
      const data = await novelService.getNovelList();
      setNovels(data);
    } catch (error) {
      console.error('Failed to load novels:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (novelId: number) => {
    if (!confirm('确定要删除这部小说吗？')) return;

    try {
      await novelService.deleteNovel(novelId);
      setNovels(novels.filter(n => n.id !== novelId));
    } catch (error) {
      console.error('Failed to delete novel:', error);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case '进行中':
        return <TrendingUp className="h-3 w-3" />;
      case '已完成':
        return <BookOpen className="h-3 w-3" />;
      default:
        return <Clock className="h-3 w-3" />;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-background to-muted/20">
      <div className="container mx-auto px-4 py-12 max-w-5xl">
        <div className="flex justify-between items-center mb-12">
          <div>
            <h1 className="text-4xl font-bold tracking-tight text-foreground mb-2">我的小说</h1>
            <p className="text-sm text-muted-foreground">
              {novels.length > 0 ? `共 ${novels.length} 部作品` : '开始你的创作之旅'}
            </p>
          </div>
          <div className="flex items-center gap-4">
            <ThemeToggle />
            <Button onClick={() => navigate('/create')} className="gap-2 shadow-lg hover:shadow-xl transition-shadow">
              <Plus className="h-4 w-4" />
              新建小说
            </Button>
          </div>
        </div>

        {loading ? (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="flex items-center justify-between p-6 border rounded-xl bg-card/50">
                <div className="flex-1 space-y-3">
                  <Skeleton className="h-6 w-1/3" />
                  <Skeleton className="h-4 w-1/4" />
                </div>
                <div className="flex gap-2">
                  <Skeleton className="h-10 w-10 rounded-lg" />
                  <Skeleton className="h-10 w-10 rounded-lg" />
                </div>
              </div>
            ))}
          </div>
        ) : novels.length === 0 ? (
          <div className="text-center py-32">
            <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-muted/50 mb-6">
              <BookOpen className="h-10 w-10 text-muted-foreground" />
            </div>
            <h3 className="text-xl font-semibold mb-2">还没有小说</h3>
            <p className="text-muted-foreground mb-8 max-w-md mx-auto">
              创建你的第一部小说，让 AI 助手帮你构建世界观、设计角色、规划剧情
            </p>
            <Button onClick={() => navigate('/create')} variant="default" size="lg" className="gap-2">
              <Plus className="h-5 w-5" />
              开始创作
            </Button>
          </div>
        ) : (
          <div className="space-y-4">
            {novels.map(novel => (
              <div
                key={novel.id}
                className="group relative flex items-center justify-between p-6 border border-border/50 rounded-xl bg-card/30 backdrop-blur-sm hover:bg-card/60 hover:border-primary/30 hover:shadow-lg transition-all duration-300 cursor-pointer"
                onClick={() => navigate(`/novel/${novel.id}/edit`)}
              >
                <div className="flex-1 min-w-0 pr-4">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="text-lg font-semibold truncate text-foreground group-hover:text-primary transition-colors">
                      {(!novel.title || novel.title === '?') ? '未命名' : novel.title}
                    </h3>
                    <Badge variant="outline" className="text-xs font-medium">
                      {(!novel.genre || novel.genre === '?') ? '未设置' : novel.genre}
                    </Badge>
                    <Badge variant="secondary" className="text-xs font-medium flex items-center gap-1">
                      {getStatusIcon(novel.status)}
                      {(!novel.status || novel.status === '?' || novel.status === 'draft') ? '初始化' : novel.status}
                    </Badge>
                  </div>
                  {novel.synopsis && (
                    <p className="text-sm text-muted-foreground line-clamp-2 leading-relaxed">
                      {novel.synopsis}
                    </p>
                  )}
                  {novel.createdAt && (
                    <p className="text-xs text-muted-foreground/60 mt-2 flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      创建于 {novel.createdAt}
                    </p>
                  )}
                </div>

                <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                  <Button
                    size="icon"
                    variant="ghost"
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(`/novel/${novel.id}/edit`);
                    }}
                    className="h-10 w-10 rounded-lg hover:bg-primary/10"
                  >
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete(novel.id);
                    }}
                    className="h-10 w-10 rounded-lg hover:bg-destructive/10 text-destructive hover:text-destructive"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default NovelList;
