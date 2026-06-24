import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { workflowService, CreateNovelRequest } from '../services/api';
import { ThemeToggle } from '@/components/theme-toggle';
import { DialogueMode } from '@/components/create-novel/DialogueMode';

function CreateNovel() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState<CreateNovelRequest>({
    title: '',
    genre: '玄幻',
    synopsis: '',
    goldenFinger: '',
    totalVolumes: 1,
    generateTopic: true,
  });

  const [result, setResult] = useState<any>(null);

  const handleSubmit = async () => {
    setLoading(true);
    setResult(null);

    try {
      const response = await workflowService.createNovel(formData);
      setResult(response);

      if (response.success) {
        setTimeout(() => {
          navigate(`/novel/${response.novelId}/edit`);
        }, 3000);
      }
    } catch (error: any) {
      console.error('Failed to create novel:', error);
      setResult({ success: false, errorMessage: error.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8 max-w-6xl">
        <div className="flex justify-between items-center mb-12">
          <h1 className="text-3xl font-light tracking-wide text-foreground">新建小说</h1>
          <ThemeToggle />
        </div>

        <DialogueMode
          formData={formData}
          onFormDataChange={(data) => {
            setFormData(prev => ({
              ...prev,
              ...data,
            }));
          }}
          onSubmit={handleSubmit}
          loading={loading}
        />

        {result && (
          <div className={`mt-8 p-6 rounded-lg border ${result.success ? 'border-green-500/30 bg-green-50/50' : 'border-destructive/30 bg-destructive/5'}`}>
            <h3 className={`font-medium ${result.success ? 'text-green-600' : 'text-destructive'}`}>
              {result.success ? '生成成功' : '生成失败'}
            </h3>
            {result.success ? (
              <div className="mt-3 space-y-1">
                <p className="text-green-600/90">小说创建成功！正在跳转到编辑页面...</p>
                <p className="text-sm text-muted-foreground">耗时: {result.durationMs}ms</p>
              </div>
            ) : (
              <p className="mt-3 text-destructive/90">错误: {result.errorMessage}</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default CreateNovel;
