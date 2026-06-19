import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { CreateNovelRequest } from '@/services/api';

const inputClassName = "flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50";

interface FormModeProps {
  formData: CreateNovelRequest;
  onChange: (field: keyof CreateNovelRequest, value: string | number | boolean) => void;
  onSubmit: () => void;
  onCancel: () => void;
  loading: boolean;
}

export function FormMode({ formData, onChange, onSubmit, onCancel, loading }: FormModeProps) {
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(); }}>
      <Card>
        <CardContent className="p-8">
          <div className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-foreground mb-2">
                小说标题 *
              </label>
              <input
                type="text"
                value={formData.title}
                onChange={(e) => onChange('title', e.target.value)}
                required
                className={inputClassName}
                placeholder="输入小说标题"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-foreground mb-2">
                小说类型 *
              </label>
              <select
                value={formData.genre}
                onChange={(e) => onChange('genre', e.target.value)}
                className={inputClassName}
              >
                <option value="玄幻">玄幻</option>
                <option value="仙侠">仙侠</option>
                <option value="都市">都市</option>
                <option value="科幻">科幻</option>
                <option value="历史">历史</option>
                <option value="游戏">游戏</option>
                <option value="悬疑">悬疑</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-foreground mb-2">
                简介
              </label>
              <Textarea
                value={formData.synopsis}
                onChange={(e) => onChange('synopsis', e.target.value)}
                rows={3}
                placeholder="简要描述小说内容"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-foreground mb-2">
                金手指设定
              </label>
              <Textarea
                value={formData.goldenFinger}
                onChange={(e) => onChange('goldenFinger', e.target.value)}
                rows={3}
                placeholder="主角的特殊能力或优势"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-foreground mb-2">
                预计卷数
              </label>
              <input
                type="number"
                value={formData.totalVolumes}
                onChange={(e) => onChange('totalVolumes', parseInt(e.target.value) || 1)}
                min={1}
                max={10}
                className={inputClassName}
              />
            </div>

            <div className="flex items-center">
              <input
                type="checkbox"
                checked={formData.generateTopic}
                onChange={(e) => onChange('generateTopic', e.target.checked)}
                className="h-4 w-4 text-primary focus:ring-primary border-input rounded"
              />
              <label className="ml-2 block text-sm text-foreground">
                自动生成赛道选题建议
              </label>
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="mt-8 flex gap-4">
        <Button
          type="submit"
          disabled={loading}
          className="flex-1"
        >
          {loading ? '生成中...' : '开始生成'}
        </Button>
        <Button
          type="button"
          variant="outline"
          onClick={onCancel}
        >
          取消
        </Button>
      </div>
    </form>
  );
}
