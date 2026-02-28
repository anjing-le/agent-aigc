#!/usr/bin/env python3
"""
爬取 Banana Prompt Quicker 网站的提示词数据
https://glidea.github.io/banana-prompt-quicker/

输出：JSON 格式的提示词数据，可导入到我们的灵感广场
"""

import json
import os
import re
import time
import hashlib
from datetime import datetime
from pathlib import Path

import requests
from bs4 import BeautifulSoup

# 配置
BASE_URL = "https://glidea.github.io/banana-prompt-quicker/"
GITHUB_RAW_BASE = "https://raw.githubusercontent.com/glidea/banana-prompt-quicker/main/"
OUTPUT_DIR = Path(__file__).parent / "output"
OUTPUT_JSON = OUTPUT_DIR / "banana_prompts.json"
OUTPUT_IMAGES_DIR = OUTPUT_DIR / "images"

# 请求头
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
}


def setup_dirs():
    """创建输出目录"""
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_IMAGES_DIR.mkdir(parents=True, exist_ok=True)
    print(f"✅ 输出目录: {OUTPUT_DIR}")


def fetch_page(url: str) -> str:
    """获取页面内容"""
    print(f"📥 正在获取: {url}")
    response = requests.get(url, headers=HEADERS, timeout=30)
    response.raise_for_status()
    return response.text


def download_image(url: str, filename: str) -> str | None:
    """下载图片到本地"""
    try:
        print(f"  🖼️ 下载图片: {filename}")
        response = requests.get(url, headers=HEADERS, timeout=30)
        response.raise_for_status()
        
        filepath = OUTPUT_IMAGES_DIR / filename
        with open(filepath, "wb") as f:
            f.write(response.content)
        
        return str(filepath)
    except Exception as e:
        print(f"  ❌ 下载失败: {e}")
        return None


def parse_prompts_from_html(html: str) -> list[dict]:
    """解析页面中的提示词卡片"""
    soup = BeautifulSoup(html, "html.parser")
    prompts = []
    
    # 查找所有提示词卡片
    # 根据 Banana 网站的结构，卡片可能在特定的容器中
    cards = soup.select(".prompt-card, .card, [class*='prompt'], [class*='card']")
    
    if not cards:
        # 尝试其他选择器
        cards = soup.find_all("div", class_=lambda x: x and ("card" in x.lower() or "prompt" in x.lower()))
    
    print(f"📋 找到 {len(cards)} 个卡片元素")
    
    for idx, card in enumerate(cards):
        try:
            prompt_data = extract_prompt_from_card(card, idx)
            if prompt_data:
                prompts.append(prompt_data)
        except Exception as e:
            print(f"  ⚠️ 解析卡片 {idx} 失败: {e}")
    
    return prompts


def extract_prompt_from_card(card, idx: int) -> dict | None:
    """从卡片元素中提取提示词数据"""
    # 提取标题
    title_elem = card.select_one("h3, h4, .title, .card-title, [class*='title']")
    title = title_elem.get_text(strip=True) if title_elem else None
    
    # 提取提示词/描述
    prompt_elem = card.select_one("p, .description, .prompt, .content, [class*='desc']")
    prompt = prompt_elem.get_text(strip=True) if prompt_elem else None
    
    # 提取图片
    img_elem = card.select_one("img")
    image_url = img_elem.get("src") if img_elem else None
    
    # 提取标签
    tags = []
    tag_elems = card.select(".tag, .label, .badge, [class*='tag']")
    for tag in tag_elems:
        tag_text = tag.get_text(strip=True)
        if tag_text:
            tags.append(tag_text)
    
    # 提取作者
    author_elem = card.select_one(".author, .user, [class*='author']")
    author = author_elem.get_text(strip=True) if author_elem else None
    
    if not prompt and not title:
        return None
    
    # 生成唯一ID
    content_hash = hashlib.md5((prompt or title or str(idx)).encode()).hexdigest()[:8]
    
    return {
        "id": f"banana_{content_hash}",
        "title": title,
        "prompt": prompt or title,
        "thumbnailUrl": image_url,
        "author": author,
        "tags": tags,
        "category": guess_category(tags, prompt or title or ""),
        "source": "banana-prompt-quicker",
        "sourceUrl": BASE_URL,
        "createdAt": datetime.now().isoformat(),
    }


def guess_category(tags: list[str], prompt: str) -> str:
    """根据标签和提示词猜测分类"""
    text = " ".join(tags + [prompt]).lower()
    
    if "视频" in text or "video" in text or "动画" in text:
        return "text2video"
    if "编辑" in text or "edit" in text or "修改" in text:
        return "image2image"
    if "风格" in text or "style" in text:
        return "style"
    if "人物" in text or "portrait" in text or "写真" in text:
        return "portrait"
    if "插画" in text or "illustration" in text or "场景" in text:
        return "illustration"
    
    return "text2image"


def try_fetch_from_github():
    """尝试从 GitHub 仓库获取数据"""
    # 很多静态网站的数据是从 JSON 文件加载的
    possible_data_urls = [
        "https://raw.githubusercontent.com/glidea/banana-prompt-quicker/main/data/prompts.json",
        "https://raw.githubusercontent.com/glidea/banana-prompt-quicker/main/src/data/prompts.json",
        "https://raw.githubusercontent.com/glidea/banana-prompt-quicker/main/public/data/prompts.json",
        "https://raw.githubusercontent.com/glidea/banana-prompt-quicker/main/prompts.json",
    ]
    
    for url in possible_data_urls:
        try:
            print(f"🔍 尝试获取: {url}")
            response = requests.get(url, headers=HEADERS, timeout=10)
            if response.status_code == 200:
                print(f"✅ 找到数据文件!")
                return response.json()
        except Exception as e:
            continue
    
    return None


def fetch_github_repo_files():
    """获取 GitHub 仓库文件列表，寻找数据文件"""
    api_url = "https://api.github.com/repos/glidea/banana-prompt-quicker/git/trees/main?recursive=1"
    
    try:
        print(f"🔍 获取仓库文件列表...")
        response = requests.get(api_url, headers=HEADERS, timeout=30)
        if response.status_code == 200:
            data = response.json()
            files = [item["path"] for item in data.get("tree", []) if item["type"] == "blob"]
            
            # 寻找可能的数据文件
            data_files = [f for f in files if f.endswith(".json") and ("prompt" in f.lower() or "data" in f.lower())]
            print(f"📋 找到可能的数据文件: {data_files}")
            
            return data_files
    except Exception as e:
        print(f"⚠️ 获取仓库文件列表失败: {e}")
    
    return []


def main():
    """主函数"""
    print("=" * 60)
    print("🍌 Banana Prompt Quicker 数据爬取工具")
    print("=" * 60)
    
    setup_dirs()
    
    prompts = []
    
    # 方法1：尝试从 GitHub 直接获取数据文件
    print("\n📦 方法1: 尝试从 GitHub 获取数据文件...")
    github_data = try_fetch_from_github()
    if github_data:
        if isinstance(github_data, list):
            prompts = github_data
        elif isinstance(github_data, dict) and "prompts" in github_data:
            prompts = github_data["prompts"]
        print(f"✅ 从 GitHub 获取到 {len(prompts)} 条数据")
    
    # 方法2：爬取网页
    if not prompts:
        print("\n🌐 方法2: 爬取网页...")
        try:
            html = fetch_page(BASE_URL)
            prompts = parse_prompts_from_html(html)
            print(f"✅ 从网页解析到 {len(prompts)} 条数据")
        except Exception as e:
            print(f"❌ 爬取网页失败: {e}")
    
    # 方法3：查找 GitHub 仓库中的数据文件
    if not prompts:
        print("\n🔍 方法3: 搜索 GitHub 仓库数据文件...")
        data_files = fetch_github_repo_files()
        for file_path in data_files:
            try:
                url = f"https://raw.githubusercontent.com/glidea/banana-prompt-quicker/main/{file_path}"
                response = requests.get(url, headers=HEADERS, timeout=10)
                if response.status_code == 200:
                    data = response.json()
                    if isinstance(data, list) and len(data) > 0:
                        prompts = data
                        print(f"✅ 从 {file_path} 获取到 {len(prompts)} 条数据")
                        break
            except:
                continue
    
    if not prompts:
        print("\n⚠️ 未能获取到提示词数据，请手动检查网站结构")
        print("💡 建议：可以手动从网站复制提示词数据")
        return
    
    # 保存数据
    print(f"\n💾 保存数据到: {OUTPUT_JSON}")
    
    # 转换为我们的格式
    formatted_prompts = []
    for item in prompts:
        # 图片URL：优先取 preview，其次 thumbnailUrl, image, thumbnail
        thumbnail_url = (
            item.get("preview") or 
            item.get("thumbnailUrl") or 
            item.get("image") or 
            item.get("thumbnail") or 
            ""
        )
        
        formatted = {
            "id": item.get("id", hashlib.md5(str(item).encode()).hexdigest()[:8]),
            "title": item.get("title") or item.get("name") or "",
            "prompt": item.get("prompt") or item.get("content") or item.get("description") or "",
            "thumbnailUrl": thumbnail_url,
            "author": item.get("author") or item.get("creator") or "Banana社区",
            "category": item.get("category") or guess_category(item.get("tags", []), item.get("prompt", "")),
            "subCategory": item.get("sub_category") or "",
            "mode": item.get("mode") or "generate",  # generate 或 edit
            "tags": item.get("tags", []),
            "contentType": "image",
            "source": "banana-prompt-quicker",
            "sourceLink": item.get("link") or "",
            "createdAt": item.get("created") or item.get("createdAt") or datetime.now().isoformat(),
        }
        if formatted["prompt"]:  # 只保留有提示词的
            formatted_prompts.append(formatted)
    
    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(formatted_prompts, f, ensure_ascii=False, indent=2)
    
    print(f"\n✅ 完成! 共保存 {len(formatted_prompts)} 条提示词")
    print(f"📁 输出文件: {OUTPUT_JSON}")
    
    # 打印示例
    if formatted_prompts:
        print("\n📋 示例数据:")
        sample = formatted_prompts[0]
        print(json.dumps(sample, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

