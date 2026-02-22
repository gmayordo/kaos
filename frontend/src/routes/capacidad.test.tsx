import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/capacidad/test')({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/capacidad/test"!</div>
}
