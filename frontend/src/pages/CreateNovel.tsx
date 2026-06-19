import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { workflowService, CreateNovelRequest } from '../services/api';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ThemeToggle } from '@/components/theme-toggle';
import { FormMode } from '@/components/create-novel/FormMode';
import { DialogueMode } from '@/components/create-novel/DialogueMode';

function CreateNovel() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'form' | 'dialogue'>('form');
  const [formData, setFormData] = useState<CreateNovelRequest>({
    title: '',
    genre: '玄幻',
    synopsis: '',
    goldenFinger: '',
    totalVolumes: 1,
    generateTopic: true,
  });

  const [result, setResult] = useState<any>(null);

  const handleFieldChange = (field: keyof CreateNovelRequest, value: string | number | boolean) => {
    setFormData(prev => ({
      ...prev,
      [field]: value,
    }));
  };

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

  const handleCancel = () => {
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-4xl font-bold text-foreground">新建小说</h1>
          <ThemeToggle />
        </div>

        <Tabs
          value={activeTab}
          onValueChange={(v) => setActiveTab(v as 'form' | 'dialogue')}
          className="mb-8"
        >
          <TabsList>
            <TabsTrigger value="form">表单模式</TabsTrigger>
            <TabsTrigger value="dialogue">对话模式</TabsTrigger>
          </TabsList>
          <TabsContent value="form">
            <FormMode
              formData={formData}
              onChange={handleFieldChange}
              onSubmit={handleSubmit}
              onCancel={handleCancel}
              loading={loading}
            />
          </TabsContent>
          <TabsContent value="dialogue">
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
          </TabsContent>
        </Tabs>

        {result && (
          <div className={`p-4 rounded-lg border ${result.success ? 'border-green-500 bg-green-50' : 'border-destructive bg-destructive/10'}`}>
            <h3 className={`font-semibold ${result.success ? 'text-green-600' : 'text-destructive'}`}>
              {result.success ? '生成成功' : '生成失败'}
            </h3>
            {result.success ? (
              <div className="mt-2 space-y-2">
                <p className="text-green-600">小说创建成功！正在跳转到编辑页面...</p>
                <p className="text-sm text-muted-foreground">耗时: {result.durationMs}ms</p>
              </div>
            ) : (
              <p className="mt-2 text-destructive">错误: {result.errorMessage}</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default CreateNovel;
