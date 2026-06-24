import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { novelService } from '../services/api';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ThemeToggle } from '@/components/theme-toggle';
import { Pencil, Trash2, Plus } from 'lucide-react';

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

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-12 max-w-5xl">
        <div className="flex justify-between items-center mb-12">
          <h1 className="text-3xl font-light tracking-wide text-foreground">我的小说</h1>
          <div className="flex items-center gap-4">
            <ThemeToggle />
            <Button onClick={() => navigate('/create')} className="gap-2">
              <Plus className="h-4 w-4" />
              新建
            </Button>
          </div>
        </div>

        {loading ? (
          <div className="space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="flex items-center justify-between p-4 border rounded-lg">
                <div className="flex-1 space-y-2">
                  <Skeleton className="h-5 w-1/3" />
                  <Skeleton className="h-4 w-1/4" />
                </div>
                <div className="flex gap-2">
                  <Skeleton className="h-9 w-9" />
                  <Skeleton className="h-9 w-9" />
                </div>
              </div>
            ))}
          </div>
        ) : novels.length === 0 ? (
          <div className="text-center py-24">
            <p className="text-muted-foreground mb-6">还没有小说</p>
            <Button onClick={() => navigate('/create')} variant="outline">
              开始创作
            </Button>
          </div>
        ) : (
          <div className="space-y-2">
            {novels.map(novel => (
              <div
                key={novel.id}
                className="group flex items-center justify-between p-4 border border-border/50 rounded-lg hover:border-border hover:bg-muted/30 transition-all"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-3 mb-1">
                    <h3 className="text-base font-medium truncate">
                      {(!novel.title || novel.title === '?') ? '未命名' : novel.title}
                    </h3>
                    <Badge variant="outline" className="text-xs">
                      {(!novel.genre || novel.genre === '?') ? '未设置' : novel.genre}
                    </Badge>
                    <Badge variant="secondary" className="text-xs">
                      {(!novel.status || novel.status === '?' || novel.status === 'draft') ? '初始化' : novel.status}
                    </Badge>
                  </div>
                  {novel.synopsis && (
                    <p className="text-sm text-muted-foreground truncate">
                      {novel.synopsis}
                    </p>
                  )}
                </div>

                <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  <Button
                    size="icon"
                    variant="ghost"
                    onClick={() => navigate(`/novel/${novel.id}/edit`)}
                    className="h-9 w-9"
                  >
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => handleDelete(novel.id)}
                    className="h-9 w-9 text-destructive hover:text-destructive"
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
