import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { useMemo } from 'react'

marked.setOptions({ gfm: true, breaks: false })

/** Renders lesson markdown safely (sanitized: lessons can be edited on the admin page). */
export function Markdown({ text, className = '' }: { text: string | null | undefined; className?: string }) {
  const html = useMemo(() => DOMPurify.sanitize(marked.parse(text ?? '', { async: false }) as string), [text])
  return <div className={`prose-cq ${className}`} dangerouslySetInnerHTML={{ __html: html }} />
}

export function CodeBlock({ code, label }: { code: string; label?: string }) {
  return (
    <figure className="my-2">
      {label && <figcaption className="text-xs text-muted mb-1">{label}</figcaption>}
      <pre className="bg-raised rounded-xl p-3 overflow-x-auto text-sm font-mono leading-relaxed whitespace-pre"><code>{code}</code></pre>
    </figure>
  )
}
