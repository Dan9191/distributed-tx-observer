import type { Node } from '@xyflow/react'

/** Возвращает абсолютную позицию ноды на канвасе (рекурсивно через parentId). */
export function absolutePosition(node: Node, allNodes: Node[]): { x: number; y: number } {
  if (!node.parentId) return node.position
  const parent = allNodes.find(n => n.id === node.parentId)
  if (!parent) return node.position
  const parentAbs = absolutePosition(parent, allNodes)
  return { x: parentAbs.x + node.position.x, y: parentAbs.y + node.position.y }
}

/** Проверяет, попадает ли точка внутрь группы (по абсолютным координатам). */
export function insideGroup(group: Node, point: { x: number; y: number }): boolean {
  const w = typeof group.style?.width === 'number' ? group.style.width : (group.measured?.width ?? 200)
  const h = typeof group.style?.height === 'number' ? group.style.height : (group.measured?.height ?? 150)
  return (
    point.x >= group.position.x && point.x <= group.position.x + w &&
    point.y >= group.position.y && point.y <= group.position.y + h
  )
}

/**
 * Для каждой step-ноды (с абсолютной позицией) проверяет, попадает ли она в какую-либо
 * группу, и устанавливает parentId + конвертирует позицию в относительную.
 */
export function attachToGroups(stepNodes: Node[], groupNodes: Node[]): Node[] {
  return stepNodes.map(step => {
    const parent = groupNodes.find(g => insideGroup(g, step.position))
    if (!parent) return step
    return {
      ...step,
      parentId: parent.id,
      position: {
        x: step.position.x - parent.position.x,
        y: step.position.y - parent.position.y,
      },
    }
  })
}
