import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/capacidad')({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/capacidad"!</div>
}
