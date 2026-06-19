import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { novelService } from '../services/api';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ThemeToggle } from '@/components/theme-toggle';
import { Pencil, Trash2 } from 'lucide-react';

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
      <div className="container mx-auto px-4 py-8">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-4xl font-bold text-foreground">我的小说</h1>
          <div className="flex items-center gap-3">
            <ThemeToggle />
            <Button onClick={() => navigate('/create')}>
              + 新建小说
            </Button>
          </div>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {Array.from({ length: 6 }).map((_, i) => (
              <Card key={i}>
                <CardHeader>
                  <Skeleton className="h-6 w-3/4" />
                </CardHeader>
                <CardContent className="space-y-2">
                  <Skeleton className="h-4 w-1/2" />
                  <Skeleton className="h-4 w-1/3" />
                </CardContent>
                <CardFooter className="gap-2">
                  <Skeleton className="h-10 flex-1" />
                  <Skeleton className="h-10 w-16" />
                </CardFooter>
              </Card>
            ))}
          </div>
        ) : novels.length === 0 ? (
          <Card className="text-center py-12">
            <CardContent>
              <p className="text-muted-foreground text-lg">还没有小说，点击"新建小说"开始创作吧</p>
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {novels.map(novel => (
              <Card key={novel.id} className="bg-muted border-border">
                <CardHeader>
                  <CardTitle>{(!novel.title || novel.title === '?') ? '未命名' : novel.title}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-muted-foreground">类型:</span>
                    <Badge variant="secondary">{(!novel.genre || novel.genre === '?') ? '未设置' : novel.genre}</Badge>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-muted-foreground">状态:</span>
                    <Badge variant="outline">{(!novel.status || novel.status === '?' || novel.status === 'draft') ? '初始化' : novel.status}</Badge>
                  </div>
                </CardContent>
                <CardFooter className="justify-end gap-2">
                  <Button
                    size="icon"
                    onClick={() => navigate(`/novel/${novel.id}/edit`)}
                  >
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="destructive"
                    size="icon"
                    onClick={() => handleDelete(novel.id)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </CardFooter>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default NovelList;
