use zwriter_cli::cli;
use zwriter_cli::config::AppConfig;
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // 初始化日志
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::from_default_env())
        .init();

    // 加载配置
    let config_path = std::path::PathBuf::from("config.toml");
    let config = AppConfig::load(&config_path)?;

    // 验证配置
    config.validate()?;

    // 运行 REPL
    cli::run_repl(config).await?;

    Ok(())
}
